/**
 *  RAB Flood Light Controller
 *
 *  Copyright 2015 Justin Miller
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
    name: "RAB Flood Light Controller",
    namespace: "justinmiller61",
    author: "Justin Miller",
    description: "App that subscribes to events from a RAB Flood Light Switch and adjusts modes accordingly.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Switch?") {
    	input "switches", "capability.switch", description: "Choose your RAB Flood Light Switches", multiple: true, required: true
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
    subscribe(switches, "switch", switchHandler)
    subscribe(switches, "manualMode", switchHandler)
    subscribe(switches, "timerMode", switchHandler)
}

def switchHandler(evt) {
	def theSwitch = switches.find { it.id == evt.device.id }
    
    /*
    if(evt.name == "switch") {
    	def events = theSwitch.events(max: 5)
        
        events.each {
        	log.debug("Event: ${it.name}, time: ${it.date}, value: ${it.value}")
        }
    }
    */
    
	log.debug("Received switch event for device ${theSwitch.id}")
    
    if(evt.name == "switch") {
    	def events = theSwitch.events(max: 50)
        
        def manualEvent = findManualPattern(events)
        def timerEvent = findTimerPattern(events)
        def offEvent = findOffPattern(events)
          
        theSwitch.clearModes()
          
        def mostRecentEvent = [ manualEvent, timerEvent, offEvent ].findResults { it }.max { it.date }
        switch(mostRecentEvent) {
        case manualEvent:
        	log.debug("We are in manual mode")
        	theSwitch.setManualModeOn()
        	break;
        case timerEvent:
        	log.debug("We are in timer mode")
        	theSwitch.setTimerModeOn()
        	break;
        default:
        	log.debug("Clearing mode")
        }
    }
}

def findManualPattern(events) {
	def event = findPattern(events, 2000, 4)
    if(event) {
    	log.debug("Found manual toggle pattern ending at ${event.date}")
    }
    event
}

def findTimerPattern(events) {
	def event =  findPattern(events, 3000, 6)
    if(event) {
    	log.debug("Found timer toggle pattern ending at ${event.date}")
    }
    event
}

def findOffPattern(events) {
	def event = findPattern(events, 10000, 2, true)
    if(event) {
    	log.debug("Found off toggle pattern ending at ${event.date}")
    }
    event
}

def findPattern(events, period, eventsInPeriod, isOffPattern = false) {
    def i = 0
    def found = events.findResult { event ->
    	def lastIndexInPeriod = i+eventsInPeriod-1
        
    	if(lastIndexInPeriod > (events.size()-1)) {
        	return -1
        } else if(event.value == "on") {
        	def theEvents = events[i..lastIndexInPeriod]

            if(isAlternatingPattern(theEvents)) {
                def first = theEvents[-1].date.time
                def last = theEvents[0].date.time
            
            	if(isOffPattern) {
                	if((last - first) >= period) {
                    	return i
                    }
                } else if((last - first) <= period) {
	                return i
                }
            }
        }
        ++i
        return null
    }
    
    return found != -1 ? events[found] : null
}

def isAlternatingPattern(events) {
	def shouldBe="on"
    
    events.find { 
        def matched = it.value == shouldBe
        shouldBe = it.value == "on" ? "off" : "on"
        matched
    }
}