package fr.renblood.npcshopkeeper.skin;

import com.google.gson.JsonObject;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Run via gradlew skinRepositoryTest; all network responses are fixtures. */
public final class SkinRepositoryTest {
    private static final String HASH="a".repeat(64);
    private static int passed;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static byte[] png(int height)throws Exception{
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(64,height,BufferedImage.TYPE_INT_ARGB),"png",out);return out.toByteArray();
    }
    private static SkinRepository.Response ok(byte[] value){return new SkinRepository.Response(200,value,null);}
    private static byte[] json(String value){return value.getBytes(StandardCharsets.UTF_8);}
    private static Path fixture(Path root)throws Exception{return Files.createTempDirectory(root,"fixture-");}
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]).toAbsolutePath().normalize();Files.createDirectories(root);
        byte[] png=png(64);AtomicLong now=new AtomicLong(System.currentTimeMillis());
        {
            Path cache=fixture(root);AtomicInteger requests=new AtomicInteger();
            var repo=new SkinRepository(cache,u->{requests.incrementAndGet();return ok(png);},now::get);
            check(Arrays.equals(repo.load(HASH),png),"Direct texture is loaded");
            check(Arrays.equals(repo.load("https://textures.minecraft.net/texture/"+HASH),png),"URL/hash aliases share persisted cache");
            var restarted=new SkinRepository(cache,u->{throw new IOException("offline");},now::get);
            check(Arrays.equals(restarted.load(HASH),png),"Cached skin survives restart and an outage");
            check(requests.get()==1,"Only one download across aliases/restarts");passed++;
        }
        {
            Path cache=fixture(root);AtomicInteger requests=new AtomicInteger();String profileId="0123456789abcdef0123456789abcdef";
            JsonObject props=new JsonObject();JsonObject textures=new JsonObject();JsonObject skin=new JsonObject();skin.addProperty("url","http://textures.minecraft.net/texture/"+HASH);textures.add("SKIN",skin);props.add("textures",textures);
            String encoded=Base64.getEncoder().encodeToString(props.toString().getBytes(StandardCharsets.UTF_8));
            var repo=new SkinRepository(cache,u->{requests.incrementAndGet();if(u.getHost().equals("api.mojang.com"))return ok(json("{\"id\":\""+profileId+"\"}"));if(u.getHost().equals("sessionserver.mojang.com"))return ok(json("{\"properties\":[{\"name\":\"textures\",\"value\":\""+encoded+"\"}]}"));return ok(png);},now::get);
            check(Arrays.equals(repo.load("totoroindahood"),png),"Official profile pipeline resolves exact PNG");
            check(Arrays.equals(repo.load("TotoroInDaHood"),png)&&requests.get()==3,"Username case variants share cached image without extra profile calls");passed++;
            // A stale cached PNG is still usable when every service is unavailable.
            try(var files=Files.list(cache)){for(Path file:files.toList())Files.setLastModifiedTime(file,FileTime.fromMillis(now.get()-2*24*60*60*1000L));}
            var offline=new SkinRepository(cache,u->{throw new IOException("offline");},now::get);
            check(offline.cached("totoroindahood")!=null&&!offline.cached("totoroindahood").fresh(),"Stale disk snapshot is available before network refresh");
            check(Arrays.equals(offline.load("totoroindahood"),png),"Stale last-good skin survives service outage");passed++;
        }
        {
            AtomicInteger official=new AtomicInteger(),fallback=new AtomicInteger();
            var repo=new SkinRepository(fixture(root),u->{if(u.getHost().equals("api.mojang.com")){official.incrementAndGet();return new SkinRepository.Response(429,new byte[0],"600");}fallback.incrementAndGet();return new SkinRepository.Response(503,new byte[0],null);},now::get);
            try{repo.load("PlayerOne");throw new AssertionError("Expected rate limit");}catch(SkinRepository.RateLimited limit){check(limit.retryMs==600_000,"Retry-After is respected");}
            try{repo.load("PlayerTwo");throw new AssertionError("Expected shared cooldown");}catch(SkinRepository.RateLimited expected){}
            check(official.get()==1,"Other NPCs do not immediately retry a throttled host");passed++;
            now.addAndGet(600_001);
            try{repo.load("PlayerOne");}catch(SkinRepository.RateLimited expected){}
            check(official.get()==2,"Host can recover after cooldown");passed++;
        }
        {
            AtomicInteger requests=new AtomicInteger();var repo=new SkinRepository(fixture(root),u->{requests.incrementAndGet();return ok(png);},now::get);
            try{repo.load("https://namemc.com/skin/a221cf1187f3c4f2");throw new AssertionError("Page is not a texture");}catch(SkinRepository.InvalidSource expected){}
            check(requests.get()==0,"NameMC pages never become invalid username requests");passed++;
        }
        {
            byte[] legacy=png(32);var repo=new SkinRepository(fixture(root),u->ok(legacy),now::get);
            check(Arrays.equals(repo.load("https://example.test/skin.png"),legacy),"Legacy 64x32 skin is accepted");passed++;
            byte[] corrupt=png.clone();corrupt[16]=1;var bad=new SkinRepository(fixture(root),u->ok(corrupt),now::get);
            try{bad.load(HASH);throw new AssertionError("Invalid dimensions");}catch(IOException expected){}passed++;
        }
        {
            SkinLoadGate gate=new SkinLoadGate();AtomicInteger winners=new AtomicInteger();var pool=Executors.newFixedThreadPool(4);
            try{var tasks=new java.util.ArrayList<java.util.concurrent.Future<?>>();for(int i=0;i<100;i++)tasks.add(pool.submit(()->{if(gate.begin("same-skin",1000))winners.incrementAndGet();}));for(var task:tasks)task.get();}finally{pool.shutdownNow();}
            check(winners.get()==1,"Only one queued/running lookup per skin");
            gate.failure("same-skin",1000,120_000);check(!gate.begin("same-skin",120999),"Failure is not retried every frame");check(gate.begin("same-skin",121000),"Failure can recover without restarting");gate.success("same-skin");check(gate.begin("same-skin",121001),"Successful state cannot remain permanently loading");passed++;
        }
        check(SkinRepository.retryDelay("999999",0)==999_999_000,"Long valid provider delays are respected");
        check(SkinRepository.retryDelay("invalid",0)==120_000,"Invalid Retry-After gets safe default");passed++;
        System.out.println("SKIN FIXTURES: "+passed+" checks passed; no external network used.");
    }
}
