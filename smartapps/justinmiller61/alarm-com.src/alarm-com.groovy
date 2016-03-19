/**
 *  Alarm.com
 *
 *  Copyright 2016 Justin Miller
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
    name: "Alarm.com",
    namespace: "justinmiller61",
    author: "Justin Miller",
    description: "Provides a web endpoint for the IFTTT Maker channel for rudimentary alarm.com integration.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
	section("Contact Sensors") {
    	input "contactSensors", "capability.contactSensor", title: "Contact Sensors?", multiple: true
	}
}

mappings {
    path("/state_change") {
        action: [
            PUT: "handleStateChange",
        ]
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
	
}

def handleStateChange() {
	def subject = request.JSON?.subject
    log.debug(subject)
    
    //1 == open
    //2 == closed
    def sensorState = 1
    def matcher = subject =~ /(?i)^Home:\s*(?:The)?(.+) was left open/
    
    if (!matcher) {
    	matcher = subject =~ /(?i)^Home:\s*(?:The)?(.+) issued a sensor.*/
        sensorState = matcher ? 2 : 0
    }
    
    if (sensorState > 0) {
    	def sensorName = matcher[0][1].trim()
        
    	def contactSensor = contactSensors.find { sensor -> sensor.displayName == sensorName }
        if (contactSensor) {
        	log.debug("Found sensor: ${contactSensor.displayName}")
            switch(sensorState) {
            case 1:
            	log.debug("${sensorName} is open")
            	contactSensor.off()
                break
            case 2:
            	log.debug("${sensorName} is closed")
                contactSensor.on()
                break
            }
        }
    }
}