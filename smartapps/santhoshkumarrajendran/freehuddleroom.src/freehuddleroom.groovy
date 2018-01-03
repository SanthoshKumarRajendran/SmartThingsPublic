/**
 *  FreeHuddleRoom
 *
 *  Copyright 2017 Santhosh Kumar Rajendran
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "FreeHuddleRoom",
    namespace: "SanthoshKumarRajendran",
    author: "Santhosh Kumar Rajendran",
    description: "Detect free Huddle Rooms",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	// Motion Sensor Settings
    section("Motion Sensors") {
        input "motionSensors", "capability.motionSensor", required: true, title: "Select sensors : ", multiple: true
    }
    
    // Motion Sensor InActivity Time
    section("Turn off when there's been no movement for selected Time") {
        input "inActivityTime", "number", required: true, title: "InActivity Time in Seconds :"
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// Initialize Room Occupancy variable (is Map the best datastructure to use here ?)
    state.roomOccupancies = []

    log.debug "Setting : $settings"
    
	// Subscribe to all the selected motion sensors
	subscribe(motionSensors, "motion", motionHandler)
}

def motionHandler(evt) {
    if (evt.value == "active") {
        motionActiveHandler(evt)
    } else if (evt.value == "inactive") {
        motionInactiveHandler(evt)
    }
}

def motionActiveHandler(evt) {
	def triggerDeviceId = evt.deviceId
    log.debug "motionActiveHandler called: $evt by device : $triggerDeviceId"
    
    // Find the device that triggered the event
    def triggerDevice = motionSensors.find{it.id == triggerDeviceId}

    log.debug "Motion detected by $triggerDevice. Setting occupancy to True"
    // state.roomOccupancies['$triggerDevice'] = True
}

def motionInactiveHandler(evt) {
	def triggerDeviceId = evt.deviceId
    log.debug "motionInactiveHandler called: $evt by device : $triggerDeviceId"
    
    // Pause for inActivityTime in seconds
	pause(inActivityTime * 1000)
    
    // Check for the motion state again
	checkMotion(triggerDeviceId)
    
    // Run checkMotion in inActivityTime seconds
    // runIn(inActivityTime, checkMotion, [data: [triggerDeviceId: triggerDeviceId]])
}

def checkMotion(triggerDeviceId) {
	// Find the device that triggered the event
    // def triggerDeviceId = data.triggerDeviceId
    def triggerDevice = motionSensors.find{it.id == triggerDeviceId}
    
    log.debug "In checkMotion method for device : $triggerDevice}"

    def motionState = triggerDevice.currentState("motion")

    if (motionState.value == "inactive") {
        // get the time elapsed between now and when the motion reported inactive
        // time is in milliseconds, so it needs to be converted to seconds
        def elapsed = ( now() - motionState.date.time ) / 1000
        
        if (elapsed >= inActivityTime) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed s): Setting room occupancy to False"
            // state.roomOccupancies['$triggerDevice'] = True
        } else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed s): Doing nothing"
        }
    } else {
        // Motion active; just log it and do nothing
        log.debug "Motion is active, do nothing and wait for inactive"
    }
}
