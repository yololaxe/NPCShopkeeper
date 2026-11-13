package fr.renblood.npcshopkeeper.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/** Client network/disk work only; no render-thread or Minecraft dependency. */
public final class SkinRepository {
    private static final Pattern HASH=Pattern.compile("[0-9a-fA-F]{64}");
    private static final Pattern NAME=Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern UUID=Pattern.compile("[0-9a-fA-F]{32}");
    private static final long FRESH_MS=24*60*60*1000L;
    private static final int MAX_BYTES=262_144;
    public record Response(int status,byte[] bytes,String retryAfter){}
    public record CachedSkin(byte[] bytes,boolean fresh){}
    @FunctionalInterface public interface Transport { Response get(URI uri)throws IOException; }
    public static final class RateLimited extends IOException {
        public final long retryMs;
        public RateLimited(long retryMs){super("Service de skins limité ; nouvelle tentative différée.");this.retryMs=retryMs;}
    }
    public static final class InvalidSource extends IOException {
        public InvalidSource(){super("Utiliser un pseudo Minecraft, un UUID, un hash Minecraft ou une URL de skin PNG directe ; une page NameMC n’est pas un fichier skin.");}
    }
    private record Source(String key,URI direct,String player,String uuid){}
    private final Path directory;
    private final Transport transport;
    private final LongSupplier clock;
    private final Map<String,Long> hostBlockedUntil=new ConcurrentHashMap<>();
    private final Map<String,Long> hostNextRequest=new ConcurrentHashMap<>();

    public SkinRepository(Path directory){this(directory,SkinRepository::http,System::currentTimeMillis);}
    public SkinRepository(Path directory,Transport transport,LongSupplier clock){this.directory=directory.toAbsolutePath().normalize();this.transport=transport;this.clock=clock;}

    public CachedSkin cached(String input)throws IOException {
        Source source=parse(input);Path file=directory.resolve(digest(source.key)+".png");
        byte[] bytes=readCache(file);
        return bytes==null?null:new CachedSkin(bytes,isFresh(source,file));
    }

    private boolean isFresh(Source source,Path file){return source.direct!=null&&source.direct.getHost().equals("textures.minecraft.net")||clock.getAsLong()-modified(file)<FRESH_MS;}

    public byte[] load(String input)throws IOException {
        Source source=parse(input);
        Path file=directory.resolve(digest(source.key)+".png");
        byte[] cached=readCache(file);
        if(cached!=null&&isFresh(source,file))return cached;
        try {
            URI image=source.direct;
            if(image==null){
                try{image=officialSkin(source);}
                catch(IOException official){
                    try{image=ashconSkin(source);}
                    catch(IOException fallback){if(official instanceof RateLimited)throw official;fallback.addSuppressed(official);throw fallback;}
                }
            }
            byte[] bytes=request(image);
            if(!validImage(bytes))throw new IOException("Le fichier reçu n’est pas un skin PNG 64×64 ou 64×32 valide.");
            writeCache(file,bytes);return bytes;
        }catch(IOException failure){
            if(cached!=null)return cached; // Keep the last good appearance during an outage.
            throw failure;
        }
    }

    private URI officialSkin(Source source)throws IOException {
        String uuid=source.uuid;
        if(uuid==null){JsonObject profile=json(request(URI.create("https://api.mojang.com/users/profiles/minecraft/"+source.player)));uuid=text(profile,"id");}
        if(!UUID.matcher(uuid).matches())throw new IOException("Profil Minecraft introuvable.");
        JsonObject profile=json(request(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/"+uuid+"?unsigned=false")));
        if(profile.has("properties")&&profile.get("properties").isJsonArray())for(var entry:profile.getAsJsonArray("properties")){
            if(!entry.isJsonObject())continue;
            JsonObject property=entry.getAsJsonObject();
            if(!"textures".equals(text(property,"name")))continue;
            try{
                JsonObject value=json(Base64.getDecoder().decode(text(property,"value")));
                return textureUri(value.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString());
            }catch(RuntimeException invalid){throw new IOException("Texture du profil Minecraft indisponible.",invalid);}
        }
        throw new IOException("Ce profil Minecraft n’a pas de skin publié.");
    }

    private URI ashconSkin(Source source)throws IOException {
        JsonObject profile=json(request(URI.create("https://api.ashcon.app/mojang/v2/user/"+(source.player==null?source.uuid:source.player))));
        try {
            JsonObject skin=profile.getAsJsonObject("textures").getAsJsonObject("skin");
            return textureUri(skin.get("url").getAsString());
        }catch(RuntimeException invalid){throw new IOException("Texture alternative indisponible.",invalid);}
    }

    private byte[] request(URI uri)throws IOException {
        String host=uri.getHost().toLowerCase(Locale.ROOT);long now=clock.getAsLong();
        long blocked=hostBlockedUntil.getOrDefault(host,0L);
        if(now<blocked)throw new RateLimited(blocked-now);
        // Spread bursts from a crowd of new NPCs. At most two workers call this.
        long wait;
        synchronized(hostNextRequest){long slot=Math.max(now,hostNextRequest.getOrDefault(host,0L));hostNextRequest.put(host,slot+500);wait=slot-now;}
        if(wait>0)try{Thread.sleep(Math.min(wait,1000));}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IOException("Chargement de skin interrompu.",interrupted);}
        blocked=hostBlockedUntil.getOrDefault(host,0L);
        if(clock.getAsLong()<blocked)throw new RateLimited(blocked-clock.getAsLong());
        Response response=transport.get(uri);
        if(response.status==429){long delay=retryDelay(response.retryAfter,clock.getAsLong());hostBlockedUntil.merge(host,clock.getAsLong()+delay,Math::max);throw new RateLimited(delay);}
        if(response.status!=200)throw new IOException("Service de skin indisponible (HTTP "+response.status+").");
        if(response.bytes==null||response.bytes.length>MAX_BYTES)throw new IOException("Réponse de skin trop volumineuse.");
        return response.bytes;
    }

    private static Response http(URI uri)throws IOException {
        HttpURLConnection connection=(HttpURLConnection)uri.toURL().openConnection();
        connection.setConnectTimeout(4000);connection.setReadTimeout(6000);connection.setRequestProperty("User-Agent","Renblood-NpcShopkeeper");
        try {
            int status=connection.getResponseCode();
            if(status!=200)return new Response(status,new byte[0],connection.getHeaderField("Retry-After"));
            if(connection.getContentLengthLong()>MAX_BYTES)throw new IOException("Réponse de skin trop volumineuse.");
            try(var stream=connection.getInputStream()){
                byte[] bytes=stream.readNBytes(MAX_BYTES+1);
                if(bytes.length>MAX_BYTES)throw new IOException("Réponse de skin trop volumineuse.");
                return new Response(status,bytes,null);
            }
        }finally{connection.disconnect();}
    }

    public static long retryDelay(String value,long now){
        long delay=120_000;
        if(value!=null)try{delay=Math.multiplyExact(Long.parseLong(value.trim()),1000);}catch(ArithmeticException|NumberFormatException invalid){
            try{delay=ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()-now;}catch(RuntimeException ignored){}
        }
        return Math.max(60_000,Math.min(Long.MAX_VALUE/4,delay));
    }

    private static Source parse(String input)throws IOException {
        if(input==null||input.isBlank()||input.length()>2048)throw new InvalidSource();
        String value=input.trim();
        if(HASH.matcher(value).matches()){String hash=value.toLowerCase(Locale.ROOT);return new Source("texture:"+hash,URI.create("https://textures.minecraft.net/texture/"+hash),null,null);}
        if(NAME.matcher(value).matches())return new Source("player:"+value.toLowerCase(Locale.ROOT),null,value,null);
        String uuid=value.replace("-","");
        if(UUID.matcher(uuid).matches())return new Source("uuid:"+uuid.toLowerCase(Locale.ROOT),null,null,uuid.toLowerCase(Locale.ROOT));
        try {
            URI uri=URI.create(value);
            if(uri.getHost()==null||!java.util.List.of("http","https").contains(uri.getScheme())||uri.getUserInfo()!=null)throw new InvalidSource();
            if(uri.getHost().equalsIgnoreCase("textures.minecraft.net")){URI texture=textureUri(value);return new Source("texture:"+texture.getPath().substring(9),texture,null,null);}
            if(uri.getPath()!=null&&uri.getPath().toLowerCase(Locale.ROOT).endsWith(".png"))return new Source("url:"+value,uri,null,null);
        }catch(IllegalArgumentException invalid){throw new InvalidSource();}
        throw new InvalidSource();
    }

    private static URI textureUri(String value)throws IOException {
        try {
            URI uri=URI.create(value);
            if(!"textures.minecraft.net".equalsIgnoreCase(uri.getHost())||uri.getPath()==null||!uri.getPath().startsWith("/texture/"))throw new InvalidSource();
            String hash=uri.getPath().substring(9);
            if(!HASH.matcher(hash).matches())throw new InvalidSource();
            return URI.create("https://textures.minecraft.net/texture/"+hash.toLowerCase(Locale.ROOT));
        }catch(IllegalArgumentException invalid){throw new InvalidSource();}
    }

    private byte[] readCache(Path file){
        try{if(Files.isRegularFile(file)&&Files.size(file)<=MAX_BYTES){byte[] bytes=Files.readAllBytes(file);if(validImage(bytes))return bytes;}}
        catch(IOException ignored){}return null;
    }
    private void writeCache(Path file,byte[] bytes){
        try {
            Files.createDirectories(directory);Path temporary=Files.createTempFile(directory,"skin-",".tmp");
            try{Files.write(temporary,bytes);Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temporary);}
            try(var files=Files.list(directory)){
                var images=files.filter(p->p.getFileName().toString().matches("[0-9a-f]{64}\\.png")).sorted(Comparator.comparingLong(SkinRepository::modified)).toList();
                for(int i=0;i<images.size()-256;i++)Files.deleteIfExists(images.get(i));
            }
        }catch(IOException ignored){/* Disk failure must not prevent rendering a downloaded skin. */}
    }
    private static long modified(Path path){try{return Files.getLastModifiedTime(path).toMillis();}catch(IOException ignored){return 0;}}
    private static boolean validImage(byte[] bytes){
        if(bytes.length<24||bytes[0]!=(byte)137||bytes[1]!=80||bytes[2]!=78||bytes[3]!=71)return false;
        // Check dimensions before decoding to reject oversized images cheaply.
        var dimensions=java.nio.ByteBuffer.wrap(bytes,16,8);int width=dimensions.getInt(),height=dimensions.getInt();
        if(width!=64||(height!=64&&height!=32))return false;
        try{var image=ImageIO.read(new ByteArrayInputStream(bytes));return image!=null&&image.getWidth()==width&&image.getHeight()==height;}catch(IOException|RuntimeException invalid){return false;}
    }
    private static JsonObject json(byte[] bytes)throws IOException{try{return JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();}catch(RuntimeException invalid){throw new IOException("Réponse de profil invalide.",invalid);}}
    private static String text(JsonObject object,String key){return object.has(key)&&object.get(key).isJsonPrimitive()?object.get(key).getAsString():"";}
    private static String digest(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
}
