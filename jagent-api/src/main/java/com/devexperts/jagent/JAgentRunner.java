package com.devexperts.jagent;

/*
 * #%L
 * JAgent API
 * %%
 * Copyright (C) 2015 - 2016 Devexperts, LLC
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;

/**
 * Utility class that helps you to run {@code JAgent} instance from {@code premain} method.
 */
public class JAgentRunner {

    // Utility class, no instance
    private JAgentRunner() {
    }

    /**
     * Runs agent under specified {@link ClassLoader class loader}.
     *
     * @param jagentClass agent class to be run. Should inherits {@code JAgent} class
     *                    and have {@code (Instrumentation inst, String agentArgs} constructor.
     * @param inst        instrumentation from {@code premain} method.
     * @param agentArgs   agent arguments from {@code premain} method.
     * @param classLoader {@link ClassLoader class loader} to be used for running agent.
     * @throws ClassNotFoundException   if specified agent class isn't found.
     * @throws IllegalArgumentException if specified agent class has invalid format.
     */
    public static void runAgent(String jagentClass, Instrumentation inst, String agentArgs, ClassLoader classLoader) throws ClassNotFoundException {
        Class<?> agentClass = classLoader.loadClass(jagentClass);
        Object agent;
        try {
            agent = agentClass.getConstructor(Instrumentation.class, String.class).newInstance(inst, agentArgs);
            agentClass.getMethod("go").invoke(agent);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Invalid agent class", e);
        }
    }
}
