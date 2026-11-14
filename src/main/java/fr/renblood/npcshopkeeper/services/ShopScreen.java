package fr.renblood.npcshopkeeper.services;
import com.google.gson.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

public final class ShopScreen extends ServiceScreen {
    final boolean editor;private JsonObject data;private int selected=-1,page,searchPage;private boolean sell,addMode,focusPriceRequested;private String focusSide="buy";private String search="";
    private EditBox name,skin,buy,sellPrice,lot,stock,restock,target,quantity,searchBox;
    private int left,right,listY,listH;private JsonArray results=new JsonArray();private final java.util.List<net.minecraft.client.gui.components.Button> rowButtons=new java.util.ArrayList<>();
    public ShopScreen(JsonObject data,boolean editor){super(editor?"Atelier du marchand":"Le comptoir du marchand");this.data=data.deepCopy();this.editor=editor;readFocus(data);if(flag(data,"loading",false)){busy=true;message="Chargement du comptoir…";}}
    public void accept(JsonObject value){busy=flag(value,"loading",false);message=busy?"Chargement du comptoir…":"";data=value.deepCopy();selected=Math.min(selected,offers().size()-1);readFocus(value);init();}
    private void readFocus(JsonObject value){if(!editor||!value.has("focus_offer"))return;String id=str(value,"focus_offer","");for(int i=0;i<offers().size();i++)if(id.equals(str(offers().get(i).getAsJsonObject(),"id",""))){selected=i;focusPriceRequested=true;focusSide=str(value,"focus_side","buy");break;}}
    private boolean needsPrice(JsonObject row){return flag(row,"buy_enabled",true)&&str(row,"buy_price","0").equals("0")||flag(row,"sell_enabled",false)&&str(row,"sell_price","0").equals("0");}
    private void focusPrice(){if(selected()==null)return;selected().addProperty("automatic",false);EditBox input=focusSide.equals("sell")?sellPrice:buy;if(input==null)return;input.setEditable(true);setFocused(input);input.setFocused(true);input.setCursorPosition(input.getValue().length());input.setHighlightPos(0);message="Saisis le prix par objet en fer, puis enregistre la boutique.";}
    private JsonArray offers(){return data.getAsJsonArray("offers");}
    private JsonObject selected(){return selected>=0&&selected<offers().size()?offers().get(selected).getAsJsonObject():null;}
    @Override protected void init(){clearWidgets();rowButtons.clear();buy=null;sellPrice=null;frame();left=x+12;right=x+w/2+8;int rw=w/2-20;listY=y+(editor?100:83);listH=h-(editor?180:158);
        if(editor){
            name=field("Nom",str(data,"name",""),left,y+36,w/2-22,64);skin=field("Skin",str(data,"skin",""),right,y+36,rw-85,256);button("Apparence",right+rw-80,y+35,80,()->{flush();var skins=data.has("skins")?data.getAsJsonArray("skins"):new JsonArray();if(skins.isEmpty()){message="Saisis le chemin d’une texture ou crée un modèle PNJ.";return;}int next=0;for(int i=0;i<skins.size();i++)if(skins.get(i).getAsString().equals(str(data,"skin","")))next=(i+1)%skins.size();data.addProperty("skin",skins.get(next).getAsString());init();});
            button(cityLabel(),left,y+61,w/2-22,()->{flush();JsonArray cities=data.getAsJsonArray("cities");String id=str(data,"city_id","");int next=0;for(int i=0;i<cities.size();i++)if(cities.get(i).getAsJsonObject().get("id").getAsString().equals(id))next=i+1;if(next>=cities.size())data.remove("city_id");else data.addProperty("city_id",cities.get(next).getAsJsonObject().get("id").getAsString());request("SHOP_PRICES",editBody());});
            button(flag(data,"active",true)?"Boutique ouverte":"Boutique fermée",right,y+61,rw,()->{flush();data.addProperty("active",!flag(data,"active",true));init();});
        }else{
            button(sell?"Vendre au marchand":"Acheter au marchand",left,y+36,w/2-22,()->{sell=!sell;selected=-1;init();});
            button("Actualiser",right,y+36,rw,()->refresh());
        }
        if(addMode){searchBox=field("Rechercher un objet",search,left,listY,w/2-22,80);searchBox.setResponder(value->{search=value;page=0;findItems();});findItems();}
        int rowCount=Math.max(1,(listH-(addMode?25:0))/29),count=addMode?results.size():offers().size();if(focusPriceRequested&&selected>=0)page=selected/rowCount;page=Math.min(page,Math.max(0,(count-1)/rowCount));
        for(int i=0;i<rowCount;i++){int index=page*rowCount+i;if(index>=count)break;final int slot=i;JsonObject row=(addMode?results:offers()).get(index).getAsJsonObject();String title=str(row,"name",str(row,"item_id","Objet"));
            rowButtons.add(button(font.plainSubstrByWidth(title,w/2-55),left+24,listY+(addMode?25:0)+i*29,w/2-46,()->{int at=page*rowCount+slot;if(addMode){if(at<results.size())add(results.get(at).getAsJsonObject());}else{flush();selected=at;init();}}));
        }
        if(!editor)button("Carnet marchand",left,y+h-48,w/2-22,()->{if(minecraft.player!=null)minecraft.player.connection.sendCommand("mc marchand");});
        int pages=Math.max(1,(count+rowCount-1)/rowCount);button("‹",left,y+h-73,30,()->{flush();page=Math.max(0,page-1);init();});button((page+1)+" / "+pages,left+34,y+h-73,w/2-90,()->{});button("›",x+w/2-45,y+h-73,30,()->{flush();page=Math.min(pages-1,page+1);init();});
        if(editor){
            button(addMode?"Retour au catalogue":"+ Ajouter des articles",left,y+h-48,w/2-22,()->{flush();addMode=!addMode;page=0;init();});
            if(addMode){
                button("Depuis mon inventaire",right,listY,rw,()->{flush();for(var r:data.getAsJsonArray("inventory"))addQuiet(r.getAsJsonObject());addMode=false;selected=offers().size()-1;init();});
                button("Prix auto pour tous",right,listY+26,rw,()->{flush();for(var r:offers())if(!ShopPriceRules.automatic(data,r.getAsJsonObject()))message="Certains articles sont sans cours : définis leur prix manuel.";init();});
                button("Dupliquer et placer",right,listY+52,rw,()->{flush();request("SHOP_DUPLICATE",editBody());});
            }else if(selected()!=null){
                JsonObject row=selected();int split=Math.max(55,(rw-6)/2);
                button(flag(row,"automatic",true)?"Prix : automatiques":"Prix : manuels",right,listY,rw,()->{flush();if(flag(row,"automatic",true))row.addProperty("automatic",false);else if(!ShopPriceRules.automatic(data,row))message="Aucun cours disponible : saisis un prix manuel en fer.";init();});
                button(flag(row,"buy_enabled",true)?"Vente : oui":"Vente : non",right,listY+24,split,()->{flush();row.addProperty("buy_enabled",!flag(row,"buy_enabled",true));init();});
                button(flag(row,"sell_enabled",false)?"Rachat : oui":"Rachat : non",right+split+6,listY+24,rw-split-6,()->{flush();row.addProperty("sell_enabled",!flag(row,"sell_enabled",false));init();});
                buy=field("Vente / fer",str(row,"buy_price","0"),right,listY+59,split,18);sellPrice=field("Rachat / fer",str(row,"sell_price","0"),right+split+6,listY+59,rw-split-6,18);buy.setEditable(!flag(row,"automatic",true));sellPrice.setEditable(!flag(row,"automatic",true));
                lot=field("Lot",str(row,"lot","1"),right,listY+94,split,2);stock=field("Stock (-1 illimité)",str(row,"stock","-1"),right+split+6,listY+94,rw-split-6,7);
                restock=field("Recharge / secondes",str(row,"restock_seconds","0"),right,listY+129,split,5);target=field("Plafond",str(row,"restock_target","0"),right+split+6,listY+129,rw-split-6,7);
                if(needsPrice(row))button("Définir le prix",right,listY+197,Math.min(130,rw),()->{flush();focusSide=flag(row,"buy_enabled",true)&&str(row,"buy_price","0").equals("0")?"buy":"sell";focusPrice();});
                button("Retirer cet article",right,Math.min(y+h-74,listY+158),rw,()->{offers().remove(selected);selected=-1;init();});
            }
            button("Enregistrer la boutique",right,y+h-48,rw,()->{try{flush();request("SHOP_SAVE",editBody());}catch(Exception invalid){error("Vérifie les montants, les quantités et le stock.");}});
        }else if(selected()!=null){
            JsonObject row=selected();quantity=field("Nombre de lots","1",right+28,listY+54,rw-56,3);
            button("−",right,listY+53,24,()->adjustLots(-1)).active=lotLimit()>0;
            button("+",right+rw-24,listY+53,24,()->adjustLots(1)).active=lotLimit()>0;
            boolean allowed=lotLimit()>0&&flag(data,"active",true)&&flag(row,sell?"sell_enabled":"buy_enabled",!sell)&&flag(row,"available",true)&&Long.parseLong(str(row,sell?"sell_price":"buy_price","0"))>0;
            button(sell?"Confirmer la vente":"Confirmer l’achat",right,listY+108,rw,()->{
                try{long lots=number(quantity.getValue(),1,Math.max(1,lotLimit())),items=Math.multiplyExact(lots,row.get("lot").getAsLong());JsonObject q=new JsonObject();q.addProperty("id",str(data,"id",""));q.addProperty("key",UUID.randomUUID().toString());q.addProperty("offer_id",str(row,"id",""));q.addProperty("price_version",str(row,"price_version",""));q.addProperty("action",sell?"SELL":"BUY");q.addProperty("quantity",items);request("SHOP_BUY",q);}catch(Exception e){error("Quantité invalide.");}
            }).active=allowed;
            if(Long.parseLong(str(row,sell?"sell_price":"buy_price","0"))<=0&&flag(data,"can_edit",false)){button("Définir le prix",right,listY+134,rw,()->{JsonObject q=new JsonObject();q.addProperty("id",str(data,"id",""));q.addProperty("edit",true);q.addProperty("focus_offer",str(row,"id",""));q.addProperty("focus_side",sell?"sell":"buy");request("SHOP_REFRESH",q);});}
        }
        if(editor&&focusPriceRequested&&buy!=null){focusPriceRequested=false;focusPrice();}
    }
    public boolean awaits(String key){return key.equals(str(data,"open_request",""));}
    private int lotLimit(){JsonObject row=selected();if(row==null)return 0;return ShopQuantityRules.limit(row.get("lot").getAsInt(),!sell&&row.has("stock")&&!row.get("stock").isJsonNull()?row.get("stock").getAsInt():null);}
    private void adjustLots(int delta){quantity.setValue(Integer.toString(ShopQuantityRules.adjust(quantity.getValue(),delta,Math.max(1,lotLimit()))));}

    private JsonObject editBody(){JsonObject body=data.deepCopy();for(String key:List.of("references","cities","inventory","skins","can_edit","entity","focus_offer","focus_side"))body.remove(key);return body;}
    private void refresh(){JsonObject q=new JsonObject();q.addProperty("id",str(data,"id",""));q.addProperty("edit",editor);request("SHOP_REFRESH",q);}
    private String cityLabel(){if(data.has("city_name"))return data.has("city_id")&&!data.get("city_id").isJsonNull()?"Ville : "+str(data,"city_name",""):"Cours commun";String id=str(data,"city_id","");if(data.has("cities"))for(var v:data.getAsJsonArray("cities")){var c=v.getAsJsonObject();if(id.equals(str(c,"id","")))return "Ville : "+str(c,"name","");}return "Aucune ville · cours commun";}
    private void flush(){if(!editor)return;if(name!=null)data.addProperty("name",name.getValue());if(skin!=null)data.addProperty("skin",skin.getValue());var row=selected();if(row==null||buy==null||addMode)return;
        row.addProperty("buy_price",Long.toString(number(buy.getValue(),0,Long.MAX_VALUE)));row.addProperty("sell_price",Long.toString(number(sellPrice.getValue(),0,Long.MAX_VALUE)));row.addProperty("lot",number(lot.getValue(),1,64));long n=number(stock.getValue(),-1,1000000);if(n<0)row.add("stock",JsonNull.INSTANCE);else row.addProperty("stock",n);row.addProperty("restock_seconds",number(restock.getValue(),0,86400));row.addProperty("restock_target",number(target.getValue(),0,1000000));
    }
    private void add(JsonObject item){flush();addQuiet(item);addMode=false;selected=offers().size()-1;init();}
    private void addQuiet(JsonObject item){if(offers().size()>=54){message="54 articles maximum.";return;}for(var v:offers())if(str(v.getAsJsonObject(),"nbt","").equals(str(item,"nbt","")))return;JsonObject row=item.deepCopy();row.addProperty("automatic",false);row.addProperty("buy_enabled",true);row.addProperty("sell_enabled",false);row.addProperty("buy_price","0");row.addProperty("sell_price","0");row.addProperty("lot",1);row.add("stock",JsonNull.INSTANCE);row.addProperty("restock_seconds",0);row.addProperty("restock_target",0);if(!ShopPriceRules.automatic(data,row))message="Prix à définir : saisis un prix manuel avant de vendre.";offers().add(row);}
    private void findItems(){results=new JsonArray();String q=search.toLowerCase(Locale.ROOT);for(var item:ForgeRegistries.ITEMS){ItemStack stack=new ItemStack(item);if(stack.isEmpty())continue;String id=ForgeRegistries.ITEMS.getKey(item).toString(),name=stack.getHoverName().getString();if(!q.isBlank()&&!id.contains(q)&&!name.toLowerCase(Locale.ROOT).contains(q))continue;JsonObject row=new JsonObject();row.addProperty("item_id",id);row.addProperty("name",name);row.addProperty("nbt",stack.save(new CompoundTag()).toString());results.add(row);if(results.size()>=100)break;}}
    private String lotPrice(long unit,long quantity){try{return money(Long.toString(Math.multiplyExact(unit,quantity)));}catch(ArithmeticException tooLarge){return "Montant trop élevé";}}
    private ItemStack stack(JsonObject row){try{return ItemStack.of(TagParser.parseTag(str(row,"nbt","{}")));}catch(Exception e){return ItemStack.EMPTY;}}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){backdrop(g);g.fill(left,listY-3,x+w/2-10,y+h-80,0xFF172B32);if(!editor)label(g,cityLabel(),right,y+63,w/2-20,MUTED);
        int rowCount=Math.max(1,(listH-(addMode?25:0))/29),count=addMode?results.size():offers().size();for(int i=0;i<rowCount;i++){int index=page*rowCount+i;if(index>=count)break;g.renderItem(stack((addMode?results:offers()).get(index).getAsJsonObject()),left+3,listY+(addMode?25:0)+i*29+2);}
        if(selected()!=null&&!addMode){var row=selected();int rw=w/2-20;if(editor){if(!flag(row,"automatic",true)&&(str(row,"buy_price","0").equals("0")&&flag(row,"buy_enabled",true)||str(row,"sell_price","0").equals("0")&&flag(row,"sell_enabled",false)))label(g,"Prix à définir · montant par objet en fer",right,listY+184,rw,GOLD);label(g,"Vente / fer",right,listY+49,rw/2,MUTED);label(g,"Rachat / fer",right+rw/2,listY+49,rw/2,MUTED);label(g,"Objets par lot",right,listY+84,rw/2,MUTED);label(g,"Stock : -1 = illimité",right+rw/2,listY+84,rw/2,MUTED);label(g,"Recharge (s) · 0 = arrêt",right,listY+119,rw/2,MUTED);label(g,"Stock plafond",right+rw/2,listY+119,rw/2,MUTED);}else{long unit=Long.parseLong(str(row,sell?"sell_price":"buy_price","0")),n=row.get("lot").getAsLong();label(g,str(row,"name","Article"),right,listY,rw,PAPER);label(g,unit>0?lotPrice(unit,n)+" / lot de "+n:"Prix à définir par l’administrateur",right,listY+17,rw,GOLD);label(g,"Stock : "+str(row,"stock","illimité")+" · "+(unit<=0?"indisponible":(str(row,"trend","STABLE").equals("UP")?"en hausse":str(row,"trend","STABLE").equals("DOWN")?"en baisse":"stable")),right,listY+33,rw,MUTED);label(g,"Nombre de lots",right,listY+44,rw,MUTED);try{label(g,unit>0?"Total : "+money(Long.toString(Math.multiplyExact(Math.multiplyExact(unit,n),number(quantity.getValue(),1,Math.max(1,lotLimit()))))):"Choisis un autre article ou contacte l’équipe.",right,listY+83,rw,GOLD);}catch(Exception ignored){}}}
        if(addMode){for(int i=0;i<rowButtons.size();i++){int at=page*rowCount+i;var b=rowButtons.get(i);b.visible=at<results.size();if(b.visible)b.setMessage(net.minecraft.network.chat.Component.literal(font.plainSubstrByWidth(str(results.get(at).getAsJsonObject(),"name",""),w/2-55)));}}
        super.render(g,mx,my,partial);
    }
}
