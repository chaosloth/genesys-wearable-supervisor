/**
 * Radius Networks, Inc.
 * http://www.radiusnetworks.com
 *
 * @author David G. Young
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.genesys.beacon;

import org.altbeacon.beacon.BeaconParser;

/**
 * A specific beacon parser designed to parse only AltBeacons from raw BLE packets detected by
 * Android.  By default, this is the only <code>BeaconParser</code> that is used by the library.
 * Additional <code>BeaconParser</code> instances can get created and registered with the library.
 * {@link BeaconParser See BeaconParser for more information.}
 */
public class ConnollyBeaconParser extends BeaconParser {
    public static final String TAG = "ConnollyBeaconParser";

    /**
     * Constructs an AltBeacon Parser and sets its layout
     */
    public ConnollyBeaconParser() {
        super();
        // Configured for Radius beacon layout
        this.setBeaconLayout("m:2-3=ff4c,i:7-22,i:23-24,i:25-26,p:27-27,d:28-28");
    }

}
