package com.devexperts.jagent.sample;

/*
 * #%L
 * Sample Transformer
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

import org.aeonbits.owner.Config;

/**
 * Configuration via {@code OWNER} framework.
 */
@Config.Sources("classpath:sample.properties")
public interface Configuration extends Config {

    @Key("sample.delete.method")
    String method();

    @Key("sample.delete.class")
    String clazz();
}
