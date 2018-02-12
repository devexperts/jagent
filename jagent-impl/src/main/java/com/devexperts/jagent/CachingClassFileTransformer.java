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

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public abstract class CachingClassFileTransformer implements ClassFileTransformer {

    protected final Log log;
    private final String agentVersion;
    private volatile String cacheDir;
    private volatile String dumpDir;

    private final ThreadLocal<Checksum> checksum = new ThreadLocal<Checksum>() {
        @Override
        protected Checksum initialValue() {
            return new CRC32();
        }
    };

    protected CachingClassFileTransformer(Log log, String agentVersion) {
        this.log = log;
        this.agentVersion = agentVersion;
    }

    @Override
    public final byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                  ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException
    {
        if (!processClass(className, loader))
            return null;
        log.debug("Transforming ", className, " loaded by ", loader);
        byte[] res = getCachedOrTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
        dumpClassIfNeeded(className, loader, res);
        log.debug("Transformed ", className, " loaded by ", loader);
        return res;
    }

    private byte[] getCachedOrTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException
    {
        try {
            if (cacheDir == null)
                return transformImpl(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
            try {
                Checksum checksum = this.checksum.get();
                checksum.update(classfileBuffer, 0, classfileBuffer.length);
                Path cachedFilePath = Paths.get(cacheDir, agentVersion, className + "#crc32=" + checksum.getValue() + ".class");
                if (Files.exists(cachedFilePath)) {
                    log.debug("Load class ", className, " loaded by ", loader, " from cached file ", cachedFilePath);
                    return Files.readAllBytes(cachedFilePath);
                }
                Files.createDirectories(cachedFilePath.getParent());
                byte[] res = transformImpl(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
                if (res == null)
                    res = classfileBuffer;
                Files.write(cachedFilePath, res);
                log.debug("Cache class ", className, " loaded by ", loader, " to ", cachedFilePath);
                return res;
            } catch (IOException e) {
                log.warn("Exception on cache reading/writing ", className, " loaded by ", loader, e);
                return transformImpl(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
            }
        } catch (Exception e) {
            log.error("Unable to transform class ", className, " loaded by ", loader, e);
            return null;
        }
    }

    private void dumpClassIfNeeded(String className, ClassLoader loader, byte[] classfileBuffer) {
        if (dumpDir == null)
            return;
        try {
            Path classPath = Paths.get(dumpDir, className + "#loaderHashCode="
                + (loader != null ? loader.hashCode() : null) + ".class");
            if (classPath.getParent() != null)
                Files.createDirectories(classPath.getParent());
            Files.write(classPath, classfileBuffer);
            log.debug("Dump class ", className, " loaded by ", loader, " to ", classPath);
        } catch (Exception e) {
            log.warn("Unable to dump class ", className, " loaded by ", loader, e);
        }
    }

    protected abstract boolean processClass(String className, ClassLoader loader);

    protected abstract byte[] transformImpl(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException;

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public void setDumpDir(String dumpDir) {
        this.dumpDir = dumpDir;
    }

    public static CachingClassFileTransformer createFromClassFileTransformer(final ClassFileTransformer transformer,
                                                                             Log log, String agentVersion)
    {
        return new CachingClassFileTransformer(log, agentVersion) {

            @Override
            protected boolean processClass(String className, ClassLoader loader) {
                return true;
            }

            @Override
            protected byte[] transformImpl(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                           ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                return transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
            }
        };
    }
}
