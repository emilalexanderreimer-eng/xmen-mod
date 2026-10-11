package com.emil.xmen.ability;

import java.util.HashMap;
import java.util.Map;

import com.emil.xmen.ability.powers.BodyPowers;
import com.emil.xmen.ability.powers.EarthPowers;
import com.emil.xmen.ability.powers.EnergyPowers;
import com.emil.xmen.ability.powers.FirePowers;
import com.emil.xmen.ability.powers.HealingPowers;
import com.emil.xmen.ability.powers.IcePowers;
import com.emil.xmen.ability.powers.LightPowers;
import com.emil.xmen.ability.powers.MindPowers;
import com.emil.xmen.ability.powers.MovementPowers;
import com.emil.xmen.ability.powers.NaturePowers;
import com.emil.xmen.ability.powers.RealityPowers;
import com.emil.xmen.ability.powers.SensePowers;
import com.emil.xmen.ability.powers.ShapeshiftPowers;
import com.emil.xmen.ability.powers.TechPowers;
import com.emil.xmen.ability.powers.WeatherPowers;

/** Every power's behaviour, by power id. Each category of powers lives in its own class in powers/. */
public final class PowerBehaviors {
    private static final Map<String, PowerBehavior> BEHAVIORS = new HashMap<>();

    static {
        EnergyPowers.register();
        FirePowers.register();
        IcePowers.register();
        WeatherPowers.register();
        EarthPowers.register();
        MovementPowers.register();
        BodyPowers.register();
        HealingPowers.register();
        SensePowers.register();
        MindPowers.register();
        ShapeshiftPowers.register();
        NaturePowers.register();
        LightPowers.register();
        RealityPowers.register();
        TechPowers.register();
    }

    private PowerBehaviors() {
    }

    public static void register(String id, PowerBehavior.Builder builder) {
        if (BEHAVIORS.containsKey(id)) {
            throw new IllegalStateException("Power registered twice: " + id);
        }
        PowerBehavior behavior = builder.build();
        BEHAVIORS.put(id, behavior);
        behavior.attributes().forEach(bonus -> PowerRuntime.trackAttribute(bonus.attribute()));
    }

    public static PowerBehavior get(String id) {
        return BEHAVIORS.get(id);
    }

    public static boolean isImplemented(String id) {
        return BEHAVIORS.containsKey(id);
    }

    public static int count() {
        return BEHAVIORS.size();
    }

    public static java.util.Set<String> ids() {
        return java.util.Collections.unmodifiableSet(BEHAVIORS.keySet());
    }
}
