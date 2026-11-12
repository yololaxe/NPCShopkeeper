package fr.renblood.npcshopkeeper.services;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
public final class SailingScreen extends ServiceScreen {
    private final String destination;private final long started=System.currentTimeMillis();private boolean reduced;
    public SailingScreen(JsonObject data){super("Traversée");destination=str(data,"destination","");}
    @Override protected boolean adaptive(){return false;}
    @Override protected void init(){frame();button(reduced?"Animation réduite":"Réduire l’animation",x+w-170,y+h-40,155,()->reduced=!reduced);if(minecraft.player!=null)minecraft.player.playSound(SoundEvents.BOAT_PADDLE_WATER,.5f,.7f);}
    @Override public boolean shouldCloseOnEsc(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xFF0C1824);double progress=Math.min(1,(System.currentTimeMillis()-started)/10000.0);int horizon=height/2;
        g.fill(0,horizon,width,height,0xFF163B4B);g.fill(0,horizon-2,width,horizon,0xFF40606A);
        int boatX=(int)(width*(.25+.5*progress)),boatY=horizon+22+(reduced?0:(int)(Math.sin(progress*30)*3));
        if(!reduced){for(int i=0;i<18;i++){int wave=(int)((i*71+progress*110)%Math.max(1,width));int wy=horizon+15+(i*19)%Math.max(1,height-horizon-20);g.fill(wave,wy,Math.min(width,wave+35),wy+1,0xFF356071);}}
        g.fill(boatX-24,boatY,boatX+24,boatY+5,0xFFAC8651);g.fill(boatX-18,boatY+5,boatX+18,boatY+9,0xFF70563B);g.fill(boatX-1,boatY-40,boatX+1,boatY,0xFFE0BE7A);
        for(int line=0;line<27;line++)g.fill(boatX+2,boatY-37+line,boatX+3+(27-line)/2,boatY-36+line,0xFFD8D1B2);
        g.drawCenteredString(font,"EN MER",width/2,Math.max(15,horizon-90),GOLD);g.drawCenteredString(font,"Vers "+destination,width/2,Math.max(32,horizon-68),PAPER);
        g.drawCenteredString(font,"Le passage est enregistré. L’arrivée est confirmée par le serveur.",width/2,height-28,MUTED);
        super.render(g,mx,my,partial);
    }
}
