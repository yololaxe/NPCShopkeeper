package fr.renblood.npcshopkeeper.services;
import com.google.gson.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import fr.renblood.npcshopkeeper.network.PacketHandler;
public abstract class ServiceScreen extends Screen {
    protected int x,y,w,h;protected boolean busy;protected String message="";protected double uiScale=1;private boolean scaled;
    protected static final int GOLD=0xFFE2BE77,INK=0xFF101D25,PAPER=0xFFECE5D5,MUTED=0xFFABB8B7;
    protected ServiceScreen(String title){super(Component.literal(title));}
    protected boolean adaptive(){return true;}
    protected void frame(){uiScale=adaptive()?Math.min(1,Math.min(width/680.0,height/460.0)):1;int vw=(int)(width/uiScale),vh=(int)(height/uiScale);w=Math.min(860,vw-16);h=Math.min(510,vh-16);x=(vw-w)/2;y=(vh-h)/2;}
    protected Button button(String label,int bx,int by,int bw,Runnable action){Button b=Button.builder(Component.literal(label),v->{if(!busy)try{action.run();}catch(IllegalArgumentException|ArithmeticException invalid){error("Vérifie les valeurs du formulaire.");}}).bounds(bx,by,bw,20).build();return addRenderableWidget(b);}
    protected EditBox field(String label,String value,int fx,int fy,int fw,int limit){EditBox e=new EditBox(font,fx,fy,fw,18,Component.literal(label));e.setMaxLength(limit);e.setValue(value);e.setHint(Component.literal(label));return addRenderableWidget(e);}
    protected void label(GuiGraphics g,String text,int lx,int ly,int max,int color){g.drawString(font,font.plainSubstrByWidth(text,max),lx,ly,color,false);}
    protected void request(String action,JsonObject body){if(busy)return;busy=true;message="Vérification…";PacketHandler.sendToServer(new ServiceRequest(action,body.toString()));}
    public void error(String value){busy=false;message=value;}
    protected void backdrop(GuiGraphics g){renderBackground(g);g.pose().pushPose();g.pose().scale((float)uiScale,(float)uiScale,1);scaled=true;g.fill(x,y,x+w,y+h,INK);g.fill(x,y,x+w,y+2,GOLD);label(g,title.getString(),x+12,y+12,w-24,GOLD);label(g,message,x+12,y+h-15,w-24,MUTED);}
    protected boolean canvasClick(double mx,double my,int button){return false;}
    @Override public boolean mouseClicked(double mx,double my,int button){mx/=uiScale;my/=uiScale;return canvasClick(mx,my,button)||super.mouseClicked(mx,my,button);}
    @Override public boolean mouseReleased(double mx,double my,int button){return super.mouseReleased(mx/uiScale,my/uiScale,button);}
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){return super.mouseDragged(mx/uiScale,my/uiScale,button,dx/uiScale,dy/uiScale);}
    @Override public boolean mouseScrolled(double mx,double my,double amount){return super.mouseScrolled(mx/uiScale,my/uiScale,amount);}
    @Override public void mouseMoved(double mx,double my){super.mouseMoved(mx/uiScale,my/uiScale);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,(int)(mx/uiScale),(int)(my/uiScale),partial);if(scaled){g.pose().popPose();scaled=false;}}
    @Override public boolean isPauseScreen(){return false;}
    protected static String str(JsonObject o,String key,String fallback){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():fallback;}
    protected static boolean flag(JsonObject o,String key,boolean fallback){return o.has(key)?o.get(key).getAsBoolean():fallback;}
    protected static long number(String value,long min,long max){long n=Long.parseLong(value);if(n<min||n>max)throw new IllegalArgumentException();return n;}
    protected String money(String v){try{return fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsIntegration.formatMoney(Long.parseLong(v));}catch(Exception e){return v+" fer";}}
    protected static String stateLabel(String state){return switch(state){case "BOOKED"->"Réservé";case "MISSED"->"À reporter gratuitement";case "SAILING"->"En mer";case "ARRIVED"->"Arrivé";case "REFUNDED"->"Remboursé";case "ARRIVAL_WAIT"->"Quai d’arrivée à vérifier";default->"Règlement à confirmer";};}
}
