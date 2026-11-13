package fr.renblood.npcshopkeeper.ambience;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
@GameTestHolder("npcshopkeeper")
@PrefixGameTestTemplate(false)
public final class AmbienceGameTests {
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void ambientBudgetSettingsAreBounded(GameTestHelper h) {
        var settings=new NpcAmbientSettings();settings.volume=Float.NaN;settings.radius=Double.POSITIVE_INFINITY;settings.minDelaySeconds=-1;settings.maxDelaySeconds=-100;settings.sanitized();
        h.assertTrue(settings.volume==.22f&&settings.radius==12&&settings.minDelaySeconds==6&&settings.maxDelaySeconds==6,"Invalid preferences cannot cause a sound storm or an unbounded radius");h.succeed();
    }
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void silentOrHiddenNpcNeverProducesAmbient(GameTestHelper h) {
        h.assertTrue(NpcAmbientSettings.audible(true,false,false,false,144,12),"Inclusive local radius");
        h.assertTrue(!NpcAmbientSettings.audible(true,true,false,false,1,12)&&!NpcAmbientSettings.audible(true,false,true,false,1,12)&&!NpcAmbientSettings.audible(true,false,false,true,1,12)&&!NpcAmbientSettings.audible(false,false,false,false,1,12),"Silent, invisible, sleeping and dead characters stay silent");
        h.assertTrue(!NpcAmbientSettings.audible(true,false,false,false,145,12),"Far characters stay silent");h.succeed();
    }
}
