package com.devexperts.jagent;

/*
 * #%L
 * JAgent Impl
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

import org.objectweb.asm.Opcodes;

public class ClassInfo {
    private static final ClassInfo[] EMPTY_INFOS = new ClassInfo[0];

    private final int access;
    private final String internalName;
    private final String internalSuperName;
    private final String[] internalInterfaceNames;

    // created on first need
    private String className;
    private ClassInfo superClassInfo;
    private ClassInfo[] interfaceInfos;

    ClassInfo(int access, String internalName, String internalSuperName, String[] internalInterfaceNames) {
        this.access = access;
        this.internalName = internalName;
        this.internalSuperName = internalSuperName;
        this.internalInterfaceNames = internalInterfaceNames;
    }

    String getInternalName() {
        return internalName;
    }

    String getClassName() {
        if (className == null)
            className = internalName.replace('/', '.');
        return className;
    }

    boolean isInterface() {
        return (access & Opcodes.ACC_INTERFACE) != 0;
    }

    // Returns null if not found or failed to load
    ClassInfo getSuperclassInfo(ClassInfoCache ciCache, ClassLoader loader) {
        if (internalSuperName == null)
            return null;
        if (superClassInfo == null)
            superClassInfo = ciCache.getOrBuildClassInfo(internalSuperName, loader);
        return superClassInfo;
    }

    // throws RuntimeException if not found or failed to load
    ClassInfo getRequiredSuperclassInfo(ClassInfoCache ciCache, ClassLoader loader) {
        if (internalSuperName == null)
            return null;
        if (superClassInfo == null)
            superClassInfo = ciCache.getOrBuildRequiredClassInfo(internalSuperName, loader);
        return superClassInfo;
    }

    // Returns null infos inside if not found or failed to load
    ClassInfo[] getInterfaceInfos(ClassInfoCache ciCache, ClassLoader loader) {
        if (interfaceInfos == null) {
            if (internalInterfaceNames == null || internalInterfaceNames.length == 0)
                interfaceInfos = EMPTY_INFOS;
            else {
                int n = internalInterfaceNames.length;
                interfaceInfos = new ClassInfo[n];
                for (int i = 0; i < n; i++)
                    interfaceInfos[i] = ciCache.getOrBuildClassInfo(internalInterfaceNames[i], loader);
            }
        }
        return interfaceInfos;
    }

    // throws RuntimeException if not found or failed to load
    ClassInfo[] getRequiredInterfaceInfos(ClassInfoCache ciCache, ClassLoader loader) {
        ClassInfo[] ii = getInterfaceInfos(ciCache, loader);
        for (int i = 0; i < ii.length; i++)
            if (ii[i] == null)
                ii[i] = ciCache.getOrBuildRequiredClassInfo(internalInterfaceNames[i], loader);
        return ii;
    }

    private boolean implementsInterface(ClassInfo that, ClassInfoCache ciCache, ClassLoader loader) {
        for (ClassInfo c = this; c != null; c = c.getRequiredSuperclassInfo(ciCache, loader)) {
            for (ClassInfo ti : c.getRequiredInterfaceInfos(ciCache, loader)) {
                if (ti.getInternalName().equals(that.getInternalName())
                        || ti.implementsInterface(that, ciCache, loader))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSubclassOf(ClassInfo that, ClassInfoCache ciCache, ClassLoader loader) {
        for (ClassInfo c = this; c != null; c = c.getRequiredSuperclassInfo(ciCache, loader)) {
            if (c.getRequiredSuperclassInfo(ciCache, loader) != null
                    && c.getRequiredSuperclassInfo(ciCache, loader).getInternalName().equals(that.getInternalName()))
            {
                return true;
            }
        }
        return false;
    }

    boolean isAssignableFrom(ClassInfo that, ClassInfoCache ciCache, ClassLoader loader) {
        return this == that
                || that.isSubclassOf(this, ciCache, loader)
                || that.implementsInterface(this, ciCache, loader);
    }

}
