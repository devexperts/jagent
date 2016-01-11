package com.devexperts.jagent;

/*
 * #%L
 * JAgent Impl
 * %%
 * Copyright (C) 2015 Devexperts, LLC
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Lightweight logger implementation.
 */
public class Log {

    private final Level level;
    private final String agentName;
    private final LogPrintWriter out;

    /**
     * Creates logger.
     *
     * @param agentName agent name
     * @param level     logging level
     * @param logFile   file to which log will be recorded, pass {@code null} to use standard output stream.
     */
    public Log(String agentName, Level level, String logFile) {
        this.level = level;
        this.agentName = agentName;
        LogPrintWriter tempOut = new LogPrintWriter(System.out);
        if (logFile != null && !logFile.isEmpty()) {
            try {
                tempOut = new LogPrintWriter(new FileOutputStream(logFile));
            } catch (FileNotFoundException | SecurityException e) {
                tempOut.println("Failed to log to file: " + e);
                e.printStackTrace(tempOut);
            }
        }
        out = tempOut;
    }

    public void log(Level level, Object msg) {
        if (level.priority >= this.level.priority) {
            out.println(msg);
        }
    }

    public void log(Level level, Object msg, Throwable t) {
        if (this.level.priority >= level.priority) {
            out.println(msg);
            t.printStackTrace(out);
        }
    }

    public void debug(Object msg) {
        log(Level.DEBUG, msg);
    }

    public void info(Object msg) {
        log(Level.INFO, msg);
    }

    public void warn(Object msg) {
        log(Level.WARN, msg);
    }

    public void warn(Object msg, Throwable t) {
        log(Level.WARN, msg, t);
    }

    public void error(Object msg) {
        log(Level.ERROR, msg);
    }

    public void error(Object msg, Throwable t) {
        log(Level.ERROR, msg, t);
    }

    public enum Level {
        DEBUG(1), INFO(2), WARN(3), ERROR(4);

        private int priority;

        Level(int priority) {
            this.priority = priority;
        }
    }

    private class LogPrintWriter extends PrintWriter {

        private final OutputStream out;

        public LogPrintWriter(OutputStream out) {
            super(new FastOutputStreamWriter(out), true);
            this.out = out;
        }

        @Override
        public void println(Object x) {
            if (x instanceof CharSequence)
                println((CharSequence) x);
            else
                println(String.valueOf(x));
        }

        @Override
        public void println(String x) {
            synchronized (out) {
                printHeader();
                super.println(x);
            }
        }

        private void println(CharSequence cs) {
            synchronized (out) {
                printHeader();
                for (int i = 0; i < cs.length(); i++)
                    super.print(cs.charAt(i));
                super.println();
            }
        }

        private void printHeader() {
            FastFmtUtil.printTimeAndDate(this, System.currentTimeMillis());
            print(' ');
            print(agentName);
            print(':');
            print(' ');
        }
    }
}
