package com.devexperts.jagent.sample;

/*
 * #%L
 * Sample Transformer
 * %%
 * Copyright (C) 2015 - 2016 Devexperts, LLC
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.devexperts.jagent.JAgent;
import com.devexperts.jagent.JAgentUtil;
import com.devexperts.jagent.Log;
import org.aeonbits.owner.ConfigFactory;

import java.lang.instrument.Instrumentation;

@SuppressWarnings("unused")
public class SampleAgent extends JAgent {

    private SampleAgent(Instrumentation inst, String agentArgs, String agentName, String agentVersion, Log log) {
        super(inst, agentName, agentVersion, log);
    }

    public static SampleAgent create(Instrumentation inst, String agentArgs) {
        String agentName = JAgentUtil.getImplTitle(SampleAgent.class);
        String agentVersion = JAgentUtil.getImplVersion(SampleAgent.class);
        Configuration cfg = ConfigFactory.create(Configuration.class, System.getProperties());
        Log.Level logLevel;
        try {
            logLevel = Log.Level.valueOf(cfg.logLevel());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid log level: " + cfg.logLevel() + ", INFO used by default");
            logLevel = Log.Level.INFO;
        }
        Log log = new Log(agentName, logLevel, cfg.logFile());
        SampleAgent agent = new SampleAgent(inst, agentArgs, agentName, agentVersion, log);
        agent.setRedefineClasses(cfg.redefine());
        agent.setIsVerboseRedefinition(cfg.verboseRedifinition());
        agent.addTransformer(new MethodDeleterTransformer(cfg.clazz(), cfg.method(), log));
        return agent;
    }
}
