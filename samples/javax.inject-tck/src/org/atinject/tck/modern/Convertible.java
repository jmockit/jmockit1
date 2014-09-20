/*
 * Copyright (C) 2009 The JSR-330 Expert Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.atinject.tck.modern;

import javax.inject.*;

import org.atinject.tck.auto.Car;
import org.atinject.tck.auto.Drivers;
import org.atinject.tck.modern.accessories.*;

/**
 * A modified version of the original {@link org.atinject.tck.auto.Convertible} class, for full initialization with
 * unmocked dependencies.
 * <p/>
 * This version of the class, along with the modified versions of its dependencies, uses injection in a "modern" style,
 * as normally practiced in a Java EE environment.
 * That is, it relies only on field injection, with no setter or constructor injection.
 */
public class Convertible implements Car {

    @Inject @Drivers Seat driversSeatA;
    @Inject @Drivers Seat driversSeatB;
    @Inject SpareTire spareTire;
    @Inject Cupholder cupholder;
    @Inject Provider<Engine> engineProvider;

    @Inject Seat fieldPlainSeat;
    @Inject @Drivers Seat fieldDriversSeat;
    @Inject Tire fieldPlainTire;
    @Inject @Named("spare") Tire fieldSpareTire;
    @Inject Provider<Seat> fieldPlainSeatProvider;
    @Inject @Drivers Provider<Seat> fieldDriversSeatProvider;
    @Inject Provider<Tire> fieldPlainTireProvider;
    @Inject @Named("spare") Provider<Tire> fieldSpareTireProvider;

    @Inject static Seat staticFieldPlainSeat;
    @Inject @Drivers static Seat staticFieldDriversSeat;
    @Inject static Tire staticFieldPlainTire;
    @Inject @Named("spare") static Tire staticFieldSpareTire;
    @Inject static Provider<Seat> staticFieldPlainSeatProvider;
    @Inject @Drivers static Provider<Seat> staticFieldDriversSeatProvider;
    @Inject static Provider<Tire> staticFieldPlainTireProvider;
    @Inject @Named("spare") static Provider<Tire> staticFieldSpareTireProvider;
}
