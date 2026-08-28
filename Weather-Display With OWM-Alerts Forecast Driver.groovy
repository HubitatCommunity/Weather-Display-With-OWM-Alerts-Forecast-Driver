/*
	Weather-Display With OWM-Alerts Forecast Driver
	Import URL: https://raw.githubusercontent.com/HubitatCommunity/Weather-Display-With-OWM-Alerts-Forecast-Driver/master/Weather-Display%20With%20OWM-Alerts%20Forecast%20Driver.groovy
	Copyright 2026 @Matthew (Scottma61)

	This driver has morphed many, many times, so the genesis is very blurry now.  It stated as a WeatherUnderground
	driver, then when they restricted their API it morphed into an APIXU driver.  When APIXU ceased it became a
	Dark Sky driver .... and now that Dark Sky is going away it is morphing into a OpenWeatherMap driver.

	Many people contributed to the creation of this driver.  Significant contributors include:
	- @Cobra who adapted it from @mattw01's work and I thank them for that!
	- @bangali for his original APIXU.COM base code that much of the early versions of this driver was
	adapted from. 
	- @bangali for his the Sunrise-Sunset.org code used to calculate illuminance/lux and the more
	recent adaptations of that code from @csteele in his continuation driver 'wx-ApiXU'.
	- @csteele (and prior versions from @bangali) for the attribute selection code.
	- @csteele for his examples on how to convert to asyncHttp calls to reduce Hub resource utilization.
	- @bangali also contributed the icon work from
	https://github.com/jebbett for new cooler 'Alternative' weather icons with icons courtesy
	of https://www.deviantart.com/vclouds/art/VClouds-Weather-Icons-179152045.
	- 'waynedgrant' for his json webservice that make the weather station data available to the driver.
	- @storageanarchy for his Dark Sky Icon mapping and some new icons to compliment the Vclouds set.
	- @nh.schottfam for lots of code clean up and optimizations.
	- @bptworld for weather.gov poll error handling.

	In addition to all the cloned code from the Hubitat community, I have heavily modified/created new
	code myself @Matthew (Scottma61) with lots of help from the Hubitat community.  If you believe you
	should have been acknowledged or received attribution for a code contribution, I will happily do so.
	While I compiled and orchestrated the driver, very little is actually original work of mine.

        - Big update from @jshimota on the OpenWeatherMap data from his OpenWeatherMap Multi-API Weather Driver

	This driver is free to use.  I do not accept donations. Please feel free to contribute to those
	mentioned here if you like this work, as it would not have been possible without them.

 *********************************************************************************************************
 *  REQUIREMENTS:  You MUST have a Personal Weather Station (PWS) and use Weather-Display software to	*
 *  capture that weather data from your network or a web server.  If you do not meet this requirement	*
 *  then this driver will not work for you.  This uses the Weather-Display data files from a webserver   *
 *  you specify in the driver preferences. I used waynedgrant's work to make those data files available  *
 *  in JSON format (https://github.com/waynedgrant/json-webservice-wdlive).							  *
 *********************************************************************************************************

	This driver is intended to pull data from data files on a web server created by Weather-Display software
	(http://www.weather-display.com).  It will also supplement forecast data from OpenWeatherMap ('OWM') 
	(https://openweathermap.org).  You will need your OWM API key to use the forecast from that sites,
	but the driver it will work without an external forecast source.

	The driver uses the Weather-Display data as the primary current weather dataset.  There are a few options you can select
	from like using your forecast source for illuminance/solar radiation/lux if you do not have those sensors.

	The driver exposes both metric and imperial measurements for you to select from.

	Licensed under the Apache License, Version 2.0 (the 'License'); you may not use this file except
	in compliance with the License. You may obtain a copy of the License at:

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
	on an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
	for the specific language governing permissions and limitations under the License.

	Last Update 08/28/2026
{ Left room below to document version changes...}

	V0.7.0	08/28/2026	Code-style modernization pass to align with the community OpenWeatherMap Multi-API Weather Driver's
						conventions, while retaining every existing attribute name and this driver's own
						Weather-Display/OWM/NWS data-fetching logic. No existing attribute, preference, or dashboard
						tile was renamed or removed.
						- Replaced the custom static, cross-device data store (myUpdData/myGetData/myGetDataBD/
						  dataStoreFLD) with Hubitat's native per-device state map at all 608 call sites.
						- Renamed LOGDEBUG/LOGINFO/LOGWARN/LOGERR to logDebug/logInfo/logWarn/logError; split the
						  single 'Enable Extended Logging' toggle into five independent toggles (Info/Error/Warn/
						  Debug/Trace) plus a Trace level, matching the new driver; Debug now auto-disables 30
						  minutes after Initialize (previously all logging auto-disabled together). Added an
						  'Expose API Key In Logging?' toggle (default OFF) so the OpenWeatherMap API key is
						  redacted from logs by default (previously always logged in full).
						- Replaced sendEventPublish() with sendIfChanged()/sendIfChangedPublish(): same
						  optional-attribute show/hide behavior as before, now also skips redundant events when a
						  value hasn't actually changed.
						- Consolidated the two near-duplicate inline 16-point wind-direction lookups (one for
						  Weather-Display data, one for OpenWeatherMap data) into one calcWindDirection() helper,
						  and extended it with optional direction image/emoji output.
						- Added (all additive, none replace an existing attribute): sun & moon altitude/azimuth
						  (currentSunAltitude/Azimuth, currentMoonAltitude/Azimuth, + Text variants); per-day moon
						  phase detail sourced from OpenWeatherMap (today/tom/tda MoonPhase value, Text, PNG image
						  URL, SVG image, emoji) plus a new currentMoonPhaseTile; wind-direction image/emoji
						  variants alongside the existing wind_direction/wind_cardinal text attributes; a
						  tile-debug-length badge option; clearAllDriverStates/clearAllAttributes/clearAllSchedules
						  commands. All new attributes are grouped under new optional-attribute toggles
						  (Sun/Moon Angles, Moon Phase Detail, Wind Direction Image) following this driver's
						  existing show/hide pattern, and default OFF like every other optional attribute here.
						- Fixed a latent bug in updateLux() referencing an undefined 'holdlux' variable in its log
						  output (should have read the computed 'lux' value) - this would throw on every poll.
						- Fixed a latent bug in initialize_poll() comparing the polling-interval preference against
						  the string 'Manual Forcecast Poll Only', which never matched the actual preference option
						  'Manual Poll Only' - selecting Manual Poll Only for either the station or forecast
						  interval silently fell back to a 3-hour schedule instead of disabling scheduled polling.
	V0.6.5	01/17/2025	Replaced icons wityhj unicode characters in dashboard tiles.
	V0.6.4	05/13/2024	Fixed API URL to pull location alerts.
    V0.6.3	04/17/2024	Fixed API URL to pull location alerts.
    V0.6.2	08/31/2023	Added pull request from @nh.schottfam to display sun 'altitude' & 'azimuth' as stand-alone optional attributes. Code cleanups.
    V0.6.1	06/03/2023	Code clean-up & corrections from @nh.schottfam (Thanks!).
    V0.6.0	05/30/2023	Changes to prevent errors and better reporting in situations where there is no sunrise or sunset.
    V0.5.9	01/23/2023	Bug fix for wind_cardinal that is creating a "No Data" response w/ 3rd party tile apps.
    V0.5.8	01/05/2023	Bug fix for myTile not showing icon when neither the 'Three day Forecast Tile' nor the 'Forecast High/Low Temperatures' Optional attributes are selected.
    V0.5.7	08/23/2022	Added user selection of 2.5 or 3.5 OWM API Key; Moved Schedule Change notice to Extended Logging.
    V0.5.6	08/22/2022	Removed the sunrise-sunset.org poll.
    V0.5.5	08/20/2022	More corrections to sunrise/sunset data when when there is a Sunrise-Sunset.org failure.
    V0.5.4	07/28/2022	Code clean-up and optimization (Thanks @nh.schottfam).
    V0.5.3	07/26/2022	Fallback to hub location defaults and estimates for Sunrise-Sunset.org failure.
    V0.5.2  06/12/2022  Both MyTile and the Three day Forecast Tile use the Icon and Text selected in the 'Condition Icon/Text for current day on MyTile & Three Day Forecast Tile' option.
    V0.5.1  06/11/2022  Corrected 3 day tile icon to hon0r user's selection of Current or Forecast icon.
    V0.5.0	06/10/2022	Corrected PoP1 & PoP2 from not displaying when Extended precipitation forecast was selected.
    V0.4.9	04/17/2022	Fallback for Sunrise-Sunset.org failure.
	V0.4.8	08/11/2021	Exposed cloud coverage forecasts.
	V0.4.7	01/26/2021	Corrected a display issue on Alerts.
	V0.4.6	12/12/2020	Changes to dahboard tile logo/hyperlinks when using weather.gov for alerts and there is an alert.
	V0.4.5	12/08/2020	Bug fix for 'forecast_textn' optional attributes.
	V0.4.4	12/03/2020	New tinyurl for icons.  Added tinyurl for weather.gov alert poll.
	V0.4.3	12/01/2020	Added ability to select Weather Alert source (none/OWM/Weather.gov {US Only}).
	V0.4.2	11/26/2020	Bug fixes.  Fix timeouts on http calls (by @nh.schottfam).
	V0.4.1	11/06/2020	Refactored the dashboard tiles.
	V0.4.0	10/31/2020	Tweaked threedayfcstTile for small screens.
	V0.3.9	10/30/2020	More code cleanups/reductions/optimizations by @nh.schottfam.
	V0.3.8	10/29/2020	Bug fixes and the usual code cleanup/reduction/optimizations by @nh.schottfam.
	V0.3.7	10/29/2020	Yet another Precip bux fix.
	V0.3.6	10/29/2020	Move today's precip back to 'Daily'.  More bux fixes.
	V0.3.5	10/28/2020	More Bux fixes for new Probability of Precipitation (PoP) from OWM.
	V0.3.4	10/28/2020	Bux fixes for new Probability of Precipitation (PoP) from OWM.
	V0.3.3	10/28/2020	Added Probability of Precipitation (PoP) from OWM.  Bug fixes and code and string reductions by @nh.schottfam).
	V0.3.2	10/27/2020	Bug fixes.
	V0.3.1	10/27/2020	Removed '+' from attribute names.  Three Day Tile now has optional 'Low/High' or 'High/Low' setting.
	V0.3.0	10/26/2020	More bug fixes on the Weather-Display JSON returns for nulls.
	V0.2.9	10/25/2020	Bug fixes for null JSON returns.
	V0.2.8	10/24/2020	Added indicator of multiple alerts in tiles. Minor bug fixes (by @nh.schottfam).
	V0.2.7	10/23/2020	Code optimizations and minor bug fixes (by @nh.schottfam).
	V0.2.6	10/22/2020	Removed 'NWS' from driver name, minor bug fixes.
	V0.2.5	10/21/2020	Improved OWM URLs in the dashboard tiles to pull in location's city code (if available).
	V0.2.4	10/21/2020	Better OWM URLs in the dashboard tiles.
	V0.2.3	10/20/2020	Correcting some Tile displays from the last update.
	V0.2.2	10/20/2020	Pulling Alerts from OWM instead of NWS.
	V0.2.1	10/19/2020	Added forecast 'Morn', 'Day', 'Eve' and 'Night' temperatures for current day and tomorrow.
	V0.2.0	10/07/2020	Change to use asynchttp for NWS alerts (by @nh.schottfam).
	V0.1.9	10/02/2020	More string constant optimizations (by @nh.schottfam)
	V0.1.8	09/27/2020	Bug fix preventing polling I introduced in V0.1.7
	V0.1.7	09/24/2020	Fix to allow for use of multiple virtual devices, More string constant optimizations (by @nh.schottfam)
	V0.1.6	09/24/2020	More string constant optimizations, and removal of white space characters (by @nh.schottfam)
	V0.1.5	09/23/2020	Removing 'urgency' restrictions from alerts poll
	V0.1.4	09/22/2020	Added forecast icon url attributes for tomorrow and day-after-tomorrow
	V0.1.3	09/21/2020	Added forecast High/Low temp attributes for tomorrow and day-after-tomorrow
	V0.1.2	09/16/2020	Removing 'severity' and 'certainty' restrictions from alerts poll
	V0.1.1	09/13/2020	Re-worked Alerts to not be dependent on api.weather.gov returning a valid response code
	V0.1.0	09/12/2020	Remov most DB accesses and string cleanup (by @nh.schottfam)
	V0.0.9	09/08/2020	Restoring 'certainty' to the weather.gov alert poll
	V0.0.8	09/08/2020	Removed 'certainty' from weather.gov alert poll
	V0.0.7	09/07/2020	Bug fix for NullPointerException on line 848
	V0.0.6	09/05/2020	Improved Alert handling for dashboard tiles, again, various bug fixes
	V0.0.5	05/07/2020	Improved Alert handling for dashboard tiles, various bug fixes
	V0.0.4	04/24/2020	Corrected update time on dashboard tile attributes
	V0.0.3	04/24/2020	Continue to work on improving null handling, various bug fixes
	V0.0.2	04/23/2020	Numerous bug fixes, checking for null and scheduling corrections
	V0.0.1	04/22/2020	Initial conversion from DarkSky.net to OWM-NWS Alerts

**ATTRIBUTES CAUTION**
The way the 'optional' attributes work:
	- Initially, only the optional attributes selected will show under 'Current States' and will be available in
	dashboards.
	- Once an attribute has been selected it too will show under 'Current States' and be available in dashboards.
	<*** HOWEVER ***> If you ever de-select the optional attribute, it will still show under 'Current States'
	and will still show as an attribute for dashboards **BUT IT'S DATA WILL NO LONGER BE REFRESHED WITH DATA
	POLLS**.  This means what is shown on the 'Current States' and dashboard tiles for de-selected attributes
	may not be current valid data.
	- To my knowledge, the only way to remove the de-selected attribute from 'Current States' and not show it as
	available in the dashboard is to delete the virtual device and create a new one AND DO NOT SELECT the
	attribute you do not want to show.
*/

//file:noinspection GroovyUnusedAssignment
//file:noinspection SpellCheckingInspection
//file:noinspection unused
//file:noinspection GroovyAssignabilityCheck
//file:noinspection GrDeprecatedAPIUsage

static String version()	{  return '0.7.0'  }
import groovy.transform.Field

metadata {
	definition (name: 'Weather-Display With OWM-Alerts Forecast Driver',
		namespace: 'Matthew',
		author: 'Scottma61',
		importUrl: 'https://raw.githubusercontent.com/HubitatCommunity/Weather-Display-With-OWM-Alerts-Forecast-Driver/master/Weather-Display%20With%20OWM-Alerts%20Forecast%20Driver.groovy') {
		
		capability 'Sensor'
		capability 'Temperature Measurement'
		capability 'Illuminance Measurement'
		capability 'Relative Humidity Measurement'
		capability 'Pressure Measurement'
		capability 'Ultraviolet Index'

		capability 'Refresh'		

		attributesMap.each
		{
			k, v -> if (v.ty)	attribute k, v.ty
		}
//The following attributes may be needed for dashboards that require these attributes,
//so they are listed here and shown by default.
		attribute 'city', sSTR			//Hubitat OpenWeather SharpTool.io SmartTiles
		attribute 'feelsLike', sNUM		//SharpTool.io SmartTiles
		attribute 'forecastIcon', sSTR	//SharpTool.io
		attribute 'localSunrise', sSTR	//SharpTool.io SmartTiles
		attribute 'localSunset', sSTR	//SharpTool.io SmartTiles
		attribute 'percentPrecip', sNUM	//SharpTool.io SmartTiles
		attribute 'pressured', sSTR		//UNSURE SharpTool.io SmartTiles
		attribute 'weather', sSTR		//SharpTool.io SmartTiles
		attribute 'weatherIcon', sSTR	//SharpTool.io SmartTiles
		attribute 'weatherIcons', sSTR	//Hubitat openWeather
		attribute 'wind', sNUM			//SharpTool.io
		attribute 'windDirection', sNUM	//Hubitat OpenWeather
		attribute 'windSpeed', sNUM		//Hubitat OpenWeather

//The attributes below are sub-groups of optional attributes.  They need to be listed here to be available
//alert
		attribute 'alert', sSTR
		attribute 'alertTile', sSTR
		attribute 'alertDescr', sSTR
		attribute 'alertSender', sSTR
		
//threedayTile
		attribute 'threedayfcstTile', sSTR

//fcstHighLow
		attribute 'forecastHigh', sNUM
		attribute 'forecastHigh1', sNUM
		attribute 'forecastHigh2', sNUM
		attribute 'forecastLow', sNUM
		attribute 'forecastLow1', sNUM
		attribute 'forecastLow2', sNUM
		attribute 'forecastMorn', sNUM
		attribute 'forecastDay', sNUM
		attribute 'forecastEve', sNUM
		attribute 'forecastNight', sNUM
		attribute 'forecastMorn1', sNUM
		attribute 'forecastDay1', sNUM
		attribute 'forecastEve1', sNUM
		attribute 'forecastNight1', sNUM
		attribute 'forecast_text1', sSTR
		attribute 'forecast_text2', sSTR
		attribute 'condition_icon_url1', sSTR
		attribute 'condition_icon_url2', sSTR				

// controlled with localSunrise
		attribute 'tw_begin', sSTR
		attribute 'sunriseTime', sSTR
		attribute 'noonTime', sSTR
		attribute 'sunsetTime', sSTR
		attribute 'tw_end', sSTR

//suncalc
		attribute 'altitude', sNUM // sun angle up from the horizon (0 on your horizon, 90 straight up)
		attribute 'azimuth', sNUM  // sun angle along the horizon (0 is N, 90 East, etc..)
        
//obspoll
		attribute 'last_poll_Forecast', sSTR
		attribute 'last_observation_Forecast', sSTR

//precipExtended
		attribute 'rainDayAfterTomorrow', sNUM
		attribute 'rainTomorrow', sNUM
        attribute 'PoP1', sNUM
        attribute 'PoP2', sNUM

//cloudExtended
		attribute 'cloudToday', sNUM
		attribute 'cloudTomorrow', sNUM
		attribute 'cloudDayAfterTomorrow', sNUM

//sunMoonAngles (new - matches the community OpenWeatherMap Multi-API Weather Driver's naming/format)
		attribute 'currentSunAltitude', sNUM
		attribute 'currentSunAzimuth', sNUM
		attribute 'currentSunAltitudeText', sSTR
		attribute 'currentSunAzimuthText', sSTR
		attribute 'currentMoonAltitude', sNUM
		attribute 'currentMoonAzimuth', sNUM
		attribute 'currentMoonAltitudeText', sSTR
		attribute 'currentMoonAzimuthText', sSTR

//moonPhaseDetail (new - per-day moon phase detail sourced from OpenWeatherMap; existing 'moonPhase' attribute is untouched)
		attribute 'todayMoonPhase', sNUM
		attribute 'tomMoonPhase', sNUM
		attribute 'tdaMoonPhase', sNUM
		attribute 'todayMoonPhaseText', sSTR
		attribute 'tomMoonPhaseText', sSTR
		attribute 'tdaMoonPhaseText', sSTR
		attribute 'todayMoonPhasePngImageUrl', sSTR
		attribute 'tomMoonPhasePngImageUrl', sSTR
		attribute 'tdaMoonPhasePngImageUrl', sSTR
		attribute 'todayMoonPhaseSvgImage', sSTR
		attribute 'tomMoonPhaseSvgImage', sSTR
		attribute 'tdaMoonPhaseSvgImage', sSTR
		attribute 'todayMoonPhaseEmojiIcon', sSTR
		attribute 'tomMoonPhaseEmojiIcon', sSTR
		attribute 'tdaMoonPhaseEmojiIcon', sSTR
		attribute 'currentMoonPhaseTile', sSTR

//windDirImage (new - image/emoji variants alongside the existing wind_direction/wind_cardinal text attributes)
		attribute 'windDirImageUrl', sSTR
		attribute 'windDirectionImage', sSTR
		attribute 'windDirectionEmojiIcon', sSTR

		command 'pollData'
		command 'clearAllDriverStates'
		command 'clearAllAttributes'
		command 'clearAllSchedules'
	}

	preferences() {
		String settingDescr = settingEnable ? '<br><i>Hide many of the optional attributes to reduce the clutter, if needed, by turning OFF this toggle.</i><br>' : '<br><i>Many optional attributes are available to you, if needed, by turning ON this toggle.</i><br>'
		String logDescr = '<br><i>Extended logging will turn off automatically after 30 minutes.</i><br>'
		section('Query Inputs'){
			input 'extSource', 'enum', title: 'Select Forecast Source', required:true, defaultValue: 1, options: [1:'Weather-Display', 2:'OpenWeatherMap']
			input 'apiKey', 'text', required: true, defaultValue: 'Type OpenWeatherMap.org API Key Here', title: 'API Key'
            input 'apiVer', 'bool', title: 'API Key Version (2.5 = OFF;   3.0 = ON)', defaultValue: false
			input 'pollIntervalStation', 'enum', title: 'Station Poll Interval', required: true, defaultValue: '3 Hours', options: ['Manual Poll Only', '1 Minute', '2 Minutes', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
			input 'pollLocationStation', 'text', required: true, title: 'Station Data File Location:', defaultValue: 'http://', description: '<i>Enter location of \'everything.php\' with a trailing \'/\'</i><br>'
			input 'pollIntervalForecast', 'enum', title: 'External Source Poll Interval (daylight)', required: true, defaultValue: '3 Hours', options: ['Manual Poll Only', '2 Minutes', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
			input 'pollIntervalForecastnight', 'enum', title: 'External Source Poll Interval (nighttime)', required: true, defaultValue: '3 Hours', options: ['Manual Poll Only', '2 Minutes', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
			input 'alertSource', 'enum', required: true, defaultValue: sONE, title: 'Weather Alert Source<br>0=None 1=OWM or 2=Weather.gov (US only)', options: [0:sZERO, 1:sONE, 2:sTWO]
			input 'tempFormat', 'enum', required: true, defaultValue: 'Fahrenheit (°F)', title: 'Display Unit - Temperature: Fahrenheit (°F) or Celsius (°C)',  options: ['Fahrenheit (°F)', 'Celsius (°C)']
			input 'TWDDecimals', 'enum', required: true, defaultValue: sZERO, title: 'Display decimals for Temp, Wind & Distance', options: [0:sZERO, 1:sONE, 2:sTWO, 3:'3', 4:'4']
			input 'PDecimals', 'enum', required: true, defaultValue: sZERO, title: 'Display decimals for Pressure', options: [0:sZERO, 1:sONE, 2:sTWO, 3:'3', 4:'4']
			input 'RDecimals', 'enum', required: true, defaultValue: sZERO, title: 'Display decimals for Rain volume', options: [0:sZERO, 1:sONE, 2:sTWO, 3:'3', 4:'4']			
			input 'datetimeFormat', 'enum', required: true, defaultValue: sONE, title: 'Display Unit - Date-Time Format',  options: [1:'m/d/yyyy 12 hour (am|pm)', 2:'m/d/yyyy 24 hour', 3:'mm/dd/yyyy 12 hour (am|pm)', 4:'mm/dd/yyyy 24 hour', 5:'d/m/yyyy 12 hour (am|pm)', 6:'d/m/yyyy 24 hour', 7:'dd/mm/yyyy 12 hour (am|pm)', 8:'dd/mm/yyyy 24 hour', 9:'yyyy/mm/dd 24 hour']
			input 'distanceFormat', 'enum', required: true, defaultValue: 'Miles (mph)', title: 'Display Unit - Distance/Speed: Miles, Kilometers or knots',  options: ['Miles (mph)', 'Kilometers (kph)', 'knots', 'meters (m/s)']
			input 'pressureFormat', 'enum', required: true, defaultValue: 'Inches', title: 'Display Unit - Pressure: Inches or Millibar',  options: ['Inches', 'Millibar', 'Hectopascal']
			input 'rainFormat', 'enum', required: true, defaultValue: 'Inches', title: 'Display Unit - Precipitation: Inches or Millimeters',  options: ['Inches', 'Millimeters']
			input 'luxjitter', 'bool', title: 'Use lux jitter control (rounding)?', defaultValue: false
//	https://tinyurl.com/icnqz/ points to https://raw.githubusercontent.com/HubitatCommunity/WeatherIcons/master/
			input 'iconLocation', 'text', required: false, defaultValue: 'https://raw.githubusercontent.com/HubitatCommunity/WeatherIcons/master/', title: 'Alternative Icon Location:'
			input 'iconType', 'bool', title: 'Condition Icon/Text for current day on MyTile & Three Day Forecast Tile: On=Current or Off=Forecast', defaultValue: false
			input 'altIconsEnable', 'bool', title: 'Use Alternative Icon set for Tomorrow/Day-After forecast icon URLs?', description: '<br><i>On (default) matches current behavior. Off uses OpenWeatherMap\'s own stock icons for the condition_icon_url1/condition_icon_url2 attributes only — the current-condition icon always uses the Alternative Icon set, since a stock-icon equivalent isn\'t available for the Weather-Display data path.</i><br>', defaultValue: true
			input 'altWindDirectionImageLoc', 'text', title: 'Optional - Wind Direction Image Location', description: '<br><i>If blank, an emoji is used instead of an image for the new windDirectionImage attribute.</i><br>', required: false
			input 'altMoonPhaseImagePath', 'text', title: 'Optional - Moon Phase Image Location', description: '<br><i>Blank uses the default Moon Phase image location (supplied by @thebearmay of the Hubitat community).</i><br>', required: false
			input 'precisionSunMoonAngles', 'enum', title: 'Display Decimal Precision - Sun/Moon Angles', options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: '<br>Choice of decimal precision for the new sun/moon altitude & azimuth attributes<br>Default: <b>0</b><br>', defaultValue: "0", required: true
			input 'displayTileMoonPhaseSVGEnable', 'bool', title: 'Use SVG (On) or PNG (Off) image in the new Moon Phase Tile', defaultValue: true
			input 'debugTileEnable', 'bool', title: 'Embed a character-count debug badge on dashboard tiles?', defaultValue: false
			input 'sourcefeelsLike', 'bool', title: 'Feelslike from Weather-Display?', defaultValue: false
			input 'sourceIllumination', 'bool', title: 'Illuminance from Weather-Display?', defaultValue: true
			input 'sourceUV', 'bool', title: 'UV from Weather-Display?', defaultValue: true
			input 'sourceWind', 'bool', title: 'Wind from Weather-Display?', defaultValue: true
			input 'altCoord', 'bool', defaultValue: false, title: 'Override Hub\'s location coordinates'
			if (altCoord) {
				input 'altLat', sSTR, title: 'Override location Latitude', required: false, defaultValue: location.latitude.toString(), description: '<br>Enter location Latitude<br>'
				input 'altLon', sSTR, title: 'Override location Longitude', required: false, defaultValue: location.longitude.toString(), description: '<br>Enter location Longitude<br>'
			}
			input 'logInfoEnable', 'bool', title: 'Logging - Enable Info Logging', defaultValue: true
			input 'logErrorEnable', 'bool', title: 'Logging - Enable Error Logging', defaultValue: true
			input 'logWarnEnable', 'bool', title: 'Logging - Enable Warning Logging', defaultValue: true
			input 'logDebugEnable', 'bool', title: 'Logging - Enable Debug Logging', description: logDescr, defaultValue: false
			input 'logTraceEnable', 'bool', title: 'Logging - Enable Trace Logging', defaultValue: false
			input 'aPIKeyExposedEnable', 'bool', title: 'Logging - Expose API Key In Logging?', description: '<br><i>Off (default) redacts the OpenWeatherMap API key from logged URLs.</i><br>', defaultValue: false
			input 'settingEnable', 'bool', title: '<b>Display All Optional Attributes</b>', description: settingDescr, defaultValue: true
//build a Selector for each mapped Attribute or group of attributes
			attributesMap.each {
				keyname, attribute ->
				if (settingEnable) {
					input keyname+'Publish', 'bool', title: attribute.title, defaultValue: attribute.defa, description: sBR+(String)attribute.d+sBR
					if(keyname == 'threedayTile') input 'threedayLH', 'bool', title: 'Three Day Temp Display', description: '<br>High/Low: On or Low/High: Off<br>', defaultValue: false
					if(keyname == 'weatherSummary') input 'summaryType', 'bool', title: 'Full Weather Summary', description: '<br>Full: on or short: off summary?<br>', defaultValue: false
				}
			}
			if (settingEnable) {
				input 'windPublish', 'bool', title: 'Wind Speed', defaultValue: sFLS, description: '<br>Display wind speed<br>'
			}
		}
	}
}

@Field static final String sNULL=(String)null
@Field static final String sAB='<a>'
@Field static final String sACB='</a>'
@Field static final String sCSPAN='</span>'
@Field static final String sBR='<br>'
@Field static final String sBLK=''
@Field static final String sSPC=' '
@Field static final String sRB='>'
@Field static final String sCOMMA=','
@Field static final String sMINUS='-'
@Field static final String sCOLON=':'
@Field static final String sZERO='0'
@Field static final String sONE='1'
@Field static final String sTWO='2'
@Field static final String sDOT='.'
@Field static final String sICON='iconLocation'
@Field static final String sTMETR='tMetric'
@Field static final String sDMETR='dMetric'
@Field static final String sPMETR='pMetric'
@Field static final String sRMETR='rMetric'
@Field static final String sTEMP='temperature'
@Field static final String sSUMLST='Summary_last_poll_time'
@Field static final String sTRU='true'
@Field static final String sFLS='false'
@Field static final String sNPNG='na.png'
@Field static final String s11D='11d.png'
@Field static final String s11N='11n.png'
@Field static final String sCTS='chancetstorms'
@Field static final String sNCTS='nt_chancetstorms'
@Field static final String sRAIN='rain'
@Field static final String sNRAIN='nt_rain'
@Field static final String sPCLDY='partlycloudy'
@Field static final String sNPCLDY='nt_partlycloudy'
@Field static final String s23='23.png'
@Field static final String s9='9.png'
@Field static final String s39='39.png'
@Field static final String sDF='°F'
@Field static final String sIMGS5='<img class="cI" src='
@Field static final String sIMGS8='<img class="cIb" src='
@Field static final String sTD='<td>'
@Field static final String sTR='<tr><td>'
@Field static final String sSTR='string'
@Field static final String sNUM='number'
@Field static final String sNCWA='No current weather alerts for this area'

// <<<<<<<<<< Begin Sunrise-Sunset Poll Routines >>>>>>>>>>
void pollSunRiseSet() {
	if(ifreInstalled()) { updated(); return }
    TimeZone tZ= TimeZone.getDefault()

	Date dnow= new Date()
	String currDate = dnow.format('yyyy-MM-dd', tZ)


    String tfmt1='HH:mm'
    Date tSunrise, tSunset
    tSunrise = (Date)todaysSunrise
    tSunrise = (!tSunrise || tSunrise == null) ? Date.parse("yyyy-MM-dd hh:mm:ss", currDate + " 00:00:00") : tSunrise

    tSunset = (Date)todaysSunset
    if(!tSunset || tSunset == null){
        String currYear = dnow.format('yyyy', tZ)
        Date mar21= Date.parse("yyyy-MM-dd", currYear + '-03-21')
        Date sep21= Date.parse("yyyy-MM-dd", currYear + '-09-21')
        Boolean isBtwn= (dnow >= mar21 && dnow < sep21)
        Date twelve59= Date.parse("yyyy-MM-dd hh:mm:ss", currDate + " 23:59:59")
        Date mid01= Date.parse("yyyy-MM-dd hh:mm:ss", currDate + " 00:00:01")
        if(altLat.toDouble() > 0.0D) {
            tSunset = isBtwn ? twelve59 : mid01
        } else {
            tSunset = !isBtwn ? twelve59 : mid01
        }
    }
    state.sunRiseSet = sNULL
    state.riseTime = tSunrise.format(tfmt1, tZ)
    state.noonTime = new Date(tSunrise.getTime() + ((tSunset.getTime() - tSunrise.getTime()).intdiv(2))).format(tfmt1, tZ)
    state.setTime = tSunset.format(tfmt1, tZ)
    state.tw_begin = new Date(tSunrise.getTime() - (25*60*1000)).format(tfmt1, tZ) // 25 minutes before sunrise
    state.tw_end = new Date(tSunset.getTime() + (25*60*1000)).format(tfmt1, tZ) // 25 minutes after sunset
    state.localSunset = tSunset.format(state.timeFormat as String, tZ)
    state.localSunrise = tSunrise.format(state.timeFormat as String, tZ)
    state.riseTime1 = new Date(tSunrise.getTime() - (60*60*24*1000)).format(tfmt1, tZ)
    state.riseTime2 = new Date(tSunrise.getTime() - (60*60*24*1000*2)).format(tfmt1, tZ)
    state.setTime1 = new Date(tSunset.getTime() - (60*60*24*1000)).format(tfmt1, tZ)
    state.setTime2 = new Date(tSunset.getTime() - (60*60*24*1000*2)).format(tfmt1, tZ)
}

// >>>>>>>>>> End Sunrise-Sunset Routines <<<<<<<<<<

// <<<<<<<<<< Begin Weather-Display Poll Routines >>>>>>>>>>
void pollWD() {
	if(ifreInstalled()) { updated(); return }
	Map ParamsWD
    ParamsWD = [ uri: pollLocationStation+'everything.php', timeout: 20 ]
	logInfo('Polling Weather-Display: ' + ParamsWD.toString())
	asynchttpGet('pollWDHandler', ParamsWD)
	return
}

void pollWDHandler(resp, data) {
    logInfo('Weather-Display Data: Status: ' + resp.getStatus())
    if(resp.getStatus() == 200 || resp.getStatus() == 207){
		Map wd
		try {
			wd = parseJson(resp.data)
		} catch (Exception e) {
			logError('pollWDHandler: failed to parse JSON response from Weather-Display, skipping this poll: ' + e.message)
			return
		}
		state.wd = wd.toString()
		logInfo('Weather-Display Data: ' + wd.toString())
		if(wd.toString()==sNULL) {
			pauseExecution(1000)
			pollWD()
		}
		doPollWD(wd)		// parse the data returned by Weather-Display
	}else{
		logWarn('Weather-Display API did not return data')
	}
}

void doPollWD(Map wd) {
// <<<<<<<<<< Begin Setup Global Variables >>>>>>>>>>
    TimeZone tZ= TimeZone.getDefault()
	state.currDate = new Date().format('yyyy-MM-dd', tZ)
	state.currTime = new Date().format('HH:mm', tZ)
	if(state.riseTime <= state.currTime && state.setTime >= state.currTime) {
		state.is_day = sTRU
	}else{
		state.is_day = sFLS
	}
	if(state.currTime < state.tw_begin || state.currTime > state.tw_end) {
		state.is_light = sFLS
	}else{
		state.is_light = sTRU
	}
	if(state.is_light != state.is_lightOld) {
		if(state.is_light==sTRU) {
			logInfo(' Switching to Daytime schedule.')
		}else{
			logInfo(' Switching to Nighttime schedule.')
		}
		initialize_poll()
		state.is_lightOld = state.is_light
	}
	Integer mult_twd = state.mult_twd==sNULL ? 1 : (state.mult_twd as String).toInteger()
	Integer mult_p = state.mult_p==sNULL ? 1 : (state.mult_p as String).toInteger()
	Integer mult_r = state.mult_r==sNULL ? 1 : (state.mult_r as String).toInteger()
	String ddisp_twd = state.ddisp_twd==sNULL ? '%3.0f' : state.ddisp_twd
	Boolean isF = state[sTMETR] == sDF
// >>>>>>>>>> End Setup Global Variables <<<<<<<<<<

// <<<<<<<<<< Begin Setup Station Variables >>>>>>>>>>
	Date sotime = new Date().parse('HH:mm dd/MM/yyyy', wd.time.time_date, tZ)
	state.sotime = sotime.toString()
	Date sutime = new Date()
	state.sutime = sutime.toString()
	state[sSUMLST] = sutime.format(state.timeFormat as String, tZ).toString()
	state.Summary_last_poll_date = sutime.format(state.dateFormat as String, tZ).toString()
// >>>>>>>>>> End Setup Station Variables <<<<<<<<<<

// <<<<<<<<<< Begin Process Only If No External Forcast Is Selected  >>>>>>>>>>
	if(extSource.toInteger() == 1){
		Date fotime = new Date().parse('HH:mm d/M/yyyy', wd.time.time_date, tZ)
		Date futime = new Date()
		state[sSUMLST] = futime.format(state.timeFormat as String, tZ).toString()
		state.Summary_last_poll_date = futime.format(state.dateFormat as String, tZ).toString()

		if(!wd.everything.weather.solar.percentage){
			state.cloud = sONE
		}else{
			if(wd.everything.weather.solar.percentage.toInteger() == 100){
				state.cloud = sONE
			}else{
				state.cloud = (100 - wd.everything.weather.solar.percentage.toInteger()).toString()
			}
		}
		Integer c_code
		switch(!wd.everything.forecast.icon.code ? 99 : wd.everything.forecast.icon.code.toInteger()) {
			case 0: c_code = 800; break;
			case 1: c_code = 800; break;
			case 2: c_code = 701; break;
			case 3: c_code = 800; break;
			case 4: c_code = 804; break;
			case 5: c_code = 800; break;
			case 6: c_code = 741; break;
			case 7: c_code = 721; break;
			case 8: c_code = 300; break;
			case 9: c_code = 800; break;
			case 10: c_code = 721; break;
			case 11: c_code = 741; break;
			case 12: c_code = 300; break;
			case 13: c_code = 803; break;
			case 14: c_code = 300; break;
			case 15: c_code = 300; break;
			case 16: c_code = 601; break;
			case 17: c_code = 211; break;
			case 18: c_code = 803; break;
			case 19: c_code = 803; break;
			case 20: c_code = 300; break;
			case 21: c_code = 300; break;
			case 22: c_code = 300; break;
			case 23: c_code = 612; break;
			case 24: c_code = 612; break;
			case 25: c_code = 601; break;
			case 26: c_code = 601; break;
			case 27: c_code = 601; break;
			case 28: c_code = 800; break;
			case 29: c_code = 210; break;
			case 30: c_code = 211; break;
			case 31: c_code = 212; break;
			case 32: c_code = 221; break;
			case 33: c_code = 800; break;
			case 34: c_code = 701; break;
			case 35: c_code = 300; break;
			defa: c_code = 999; break;
		}
		state.condition_id = c_code.toString()
		state.condition_code = getCondCode((state.condition_id as String).toInteger(), state.is_day as String)
		state.condition_text = !wd.everything.forecast.icon.text ? sBLK : wd.everything.forecast.icon.text
		updateLux(false)
// <<<<<<<<<< Begin Icon processing >>>>>>>>>>
		String imgName = getImgName((state.condition_id as String).toInteger(), state.is_day as String)
		sendIfChangedPublish(name: 'condition_icon', value: sIMGS5 + imgName + '>')
		sendIfChangedPublish(name: 'condition_iconWithText', value: sIMGS5 + imgName + '><br>' + state.condition_text)
		sendIfChangedPublish(name: 'condition_icon_url', value: imgName)
		state.condition_icon_url = imgName
		sendIfChangedPublish(name: 'condition_icon_only', value: imgName.split('/')[-1].replaceFirst('\\?raw=true',sBLK))
// >>>>>>>>>> End Icon Processing <<<<<<<<<<
		String Summary_forecastTemp = '. '
		String Summary_vis = sBLK
	}
// >>>>>>>>>> End Process Only If No External Forecast Is Selected  <<<<<<<<<<

// <<<<<<<<<< Begin Process Standard Weather-Station Variables (Regardless of Forecast Selection)  >>>>>>>>>>
	state.dewpoint = (state[sTMETR]==sDF ? !wd.everything.weather.dew_point.current.f ? 0 : wd.everything.weather.dew_point.current.f.toBigDecimal() : !wd.everything.weather.dew_point.current.c==sNULL ? 0 : wd.everything.weather.dew_point.current.c.toBigDecimal()).toString()
	state.humidity = (!wd.everything.weather.humidity.current ? 0 : wd.everything.weather.humidity.current.toBigDecimal()).toString()
	state.rainToday = (state[sRMETR]=='in' ? !wd.everything.weather.rainfall.daily.in ? 0 : wd.everything.weather.rainfall.daily.in.toBigDecimal() : !wd.everything.weather.rainfall.daily.mm ? 0 : wd.everything.weather.rainfall.daily.mm.toBigDecimal()).toString()
	state.pressure = (state[sPMETR]=='inHg' ? !wd.everything.weather.pressure.current.inhg ? 0 : wd.everything.weather.pressure.current.inhg.toBigDecimal() : !wd.everything.weather.pressure.current.mb ? 0 : wd.everything.weather.pressure.current.mb.toBigDecimal()).toString()
	state.temperature = (state[sTMETR]==sDF ? !wd.everything.weather.temperature.current.f ? 0 : wd.everything.weather.temperature.current.f.toBigDecimal() : !wd.everything.weather.temperature.current.c ? 0 : wd.everything.weather.temperature.current.c.toBigDecimal()).toString()

// <<<<<<<<<< Begin Process Only If Wind from WD Is Selected  >>>>>>>>>>
	if(sourceWind==true){
		state.wind_bft_icon = 'wb' + (!wd.everything.weather.wind.avg_speed.bft ? sZERO : wd.everything.weather.wind.avg_speed.bft.toInteger().toString()) + '.png'
		String w_string_bft=sNULL
		switch(!wd.everything.weather.wind.avg_speed.bft ? 0 : wd.everything.weather.wind.avg_speed.bft.toInteger()){
			case 0: w_string_bft = 'Calm'; break;
			case 1: w_string_bft = 'Light air'; break;
			case 2: w_string_bft = 'Light breeze'; break;
			case 3: w_string_bft = 'Gentle breeze'; break;
			case 4: w_string_bft = 'Moderate breeze'; break;
			case 5: w_string_bft = 'Fresh breeze'; break;
			case 6: w_string_bft = 'Strong breeze'; break;
			case 7: w_string_bft = 'High wind, moderate gale, near gale'; break;
			case 8: w_string_bft = 'Gale, fresh gale'; break;
			case 9: w_string_bft = 'Strong/severe gale'; break;
			case 10: w_string_bft = 'Storm, whole gale'; break;
			case 11: w_string_bft = 'Violent storm'; break;
			case 12: w_string_bft = 'Hurricane force'; break;
			defa: w_string_bft = 'Calm'; break;
		}
		BigDecimal t_wd, t_wg
		if(state[sDMETR] == 'MPH') {
			t_wd = !wd.everything.weather.wind.avg_speed.mph ? 0 : Math.round(wd.everything.weather.wind.avg_speed.mph.toBigDecimal() * mult_twd) / mult_twd
			t_wg = !wd.everything.weather.wind.gust_speed.mph ? 0 : Math.round(wd.everything.weather.wind.gust_speed.mph.toBigDecimal() *  mult_twd) / mult_twd
		} else if(state[sDMETR] == 'KPH') {
			t_wd = !wd.everything.weather.wind.avg_speed.kmh ? 0 : Math.round(wd.everything.weather.wind.avg_speed.kmh.toBigDecimal() * mult_twd) / mult_twd
			t_wg = !wd.everything.weather.wind.gust_speed.kmh ? 0 : Math.round(wd.everything.weather.wind.gust_speed.kmh.toBigDecimal() * 1.609344 * mult_twd) / mult_twd
		} else if(state[sDMETR] == 'knots') {
			t_wd = !wd.everything.weather.wind.avg_speed.mph ? 0 : Math.round(wd.everything.weather.wind.avg_speed.mph.toBigDecimal() * 0.868976 * mult_twd) / mult_twd
			t_wg = !wd.everything.weather.wind.gust_speed.mph ? 0 : Math.round(wd.everything.weather.wind.gust_speed.mph.toBigDecimal() * 0.868976 * mult_twd) / mult_twd
		}else{  //  this leave only m/s
			t_wd = !wd.everything.weather.wind.avg_speed.mph ? 0 : Math.round(wd.everything.weather.wind.avg_speed.mph.toBigDecimal() *  0.44704 * mult_twd) / mult_twd
			t_wg = !wd.everything.weather.wind.gust_speed.mph ? 0 : Math.round(wd.everything.weather.wind.gust_speed.mph.toBigDecimal()  * 0.44704 * mult_twd) / mult_twd
		}
		state.wind = t_wd.toString()
		state.wind_gust = t_wg.toString()

		state.wind_degree = !wd.everything.weather.wind.direction.degrees ? sZERO : wd.everything.weather.wind.direction.degrees.toInteger().toString()
		Map wDir = calcWindDirection(!wd.everything.weather.wind.direction.degrees ? 0 : wd.everything.weather.wind.direction.degrees.toBigDecimal())
		state.wind_direction = wDir.full
		state.wind_cardinal = !wd.everything.weather.wind.direction.cardinal ? 'N' : wd.everything.weather.wind.direction.cardinal.toUpperCase()
		state.windDirImageUrl = wDir.iconUrl
		state.windDirectionImage = wDir.icon
		state.windDirectionEmojiIcon = wDir.emoji
		state.wind_string = w_string_bft + ' from the ' + state.wind_direction + (myGetDataBD('wind') < 1.0 ? sBLK: ' at ' + String.format(ddisp_twd, myGetDataBD('wind')) + sSPC + state[sDMETR])
	}
// >>>>>>>>>> End Process Only If Wind from WD Is Selected <<<<<<<<<<

	state.city = !wd.station.name ? sBLK : wd.station.name.split(/ /)[0]
	state.state = !wd.station.name ? sBLK : wd.station.name.split(/ /)[1]
	state.country = !wd.station.name ? sBLK : wd.station.name.split(/ /)[2]

	state.moonAge = !wd.everything.astronomy.moon.moon_age ? sZERO : wd.everything.astronomy.moon.moon_age.toBigDecimal().toString()
	String mPhase
	BigDecimal tma = !wd.everything.astronomy.moon.moon_age ? 0 : wd.everything.astronomy.moon.moon_age.toBigDecimal()
	if (tma >= 0 && tma < 4) {mPhase = 'New Moon'}
	else if (tma >= 4 && tma < 7) {mPhase = 'Waxing Crescent'}
	else if (tma >= 7 && tma < 10) {mPhase = 'First Quarter'}
	else if (tma >= 10 && tma < 14) {mPhase = 'Waxing Gibbous'}
	else if (tma >= 14 && tma < 18) {mPhase = 'Full Moon'}
	else if (tma >= 18 && tma < 22) {mPhase = 'Waning Gibbous'}
	else if (tma >= 22 && tma < 26) {mPhase = 'Last Quarter'}
	else if (tma >= 26) {mPhase = 'Waning Cresent'}
	state.moonPhase = mPhase
	if(solarradiationPublish){
		if(!wd.everything.weather.solar.irradiance.wm2){
			state.solarradiation = 'This station does not send Solar Radiation data.'
		}else{
			state.solarradiation = wd.everything.weather.solar.irradiance.wm2.toInteger().toString()
		}
	}
// >>>>>>>>>> End Process Standard Weather-Station Variables (Regardless of Forecast Selection)  <<<<<<<<<<

// <<<<<<<<<< Begin Process Only If Illumination from WD Is Selected  >>>>>>>>>>
	if(sourceIllumination == true){
		if (!wd.everything.weather.solar.irradiance.wm2){
			state.illuminance = 'This station does not send illuminance data.'
			state.illuminated = 'This station does not send illuminance data.'
		}else{
			BigDecimal slux = Math.max(((wd.everything.weather.solar.irradiance.wm2.toBigDecimal() / 0.0079) / 12.8),5.000) //SolarRad to Lux conversion
			state.illuminance = slux.toInteger().toString()
			state.illuminated = String.format('%,4d', slux.toInteger()).toString()
		}
	}
// >>>>>>>>>> End Process Only If Illumination from WD Is Selected  <<<<<<<<<<

// <<<<<<<<<< Begin Process Only If Ultraviolet Index from WD Is Selected  >>>>>>>>>>
	if(sourceUV==true){
		if(!wd.everything.weather.uv.uvi){
			state.ultravioletIndex = 'This station does not send ultravoilet index data.'
		}else{
			state.ultravioletIndex = wd.everything.weather.uv.uvi.toBigDecimal().toString()
		}
	}
// >>>>>>>>>> End Process Only If UV from WD Is Selected  <<<<<<<<<<

// <<<<<<<<<< Begin Process Only If feelsLike from WD Is Selected  >>>>>>>>>>
	if(sourcefeelsLike==true){
		BigDecimal t_fl
		if(state[sTMETR] == sDF) {
			t_fl = !wd.everything.weather.apparent_temperature.current.f ? 0 : Math.round(wd.everything.weather.apparent_temperature.current.f.toBigDecimal() * mult_twd) / mult_twd
		}else{
			t_fl = !wd.everything.weather.apparent_temperature.current.c ? 0 : Math.round(wd.everything.weather.apparent_temperature.current.c.toBigDecimal() * mult_twd) / mult_twd
		}
		state.feelsLike = t_fl.toString()
	}
// >>>>>>>>>> End Process Only If feelsLike from WD Is Selected  <<<<<<<<<<
	if(state.forecastPoll == sFLS){
		if(extSource.toInteger() == 2){ pollOWM() }
	}else{
		PostPoll()
	}
	return
}
// >>>>>>>>>> End Weather-Display routines <<<<<<<<<<

// <<<<<<<<<< Begin OWM Poll Routines >>>>>>>>>>
void pollOWM() {
	if(ifreInstalled()) { updated(); return }
	if( apiKey == null ) {
		logWarn('OpenWeatherMap API Key not found.  Please configure in preferences.')
		return
	}
/*  for testing a different Lat/Lon location uncommnent the two lines below */
//	String altLat = "38.627003" //"44.809122" // "40.6" //"30.6953657"
//	String altLon = "-90.199402" //"-68.735892" // "-75.43" //-88.0398912"

	Map ParamsOWM
	ParamsOWM = [ uri: 'https://api.openweathermap.org/data/' + (apiVer==true ? '3.0' : '2.5') + '/onecall?lat=' + (String)altLat + '&lon=' + (String)altLon + '&exclude=minutely,hourly&mode=json&units=imperial&appid=' + apiKey, timeout: 20 ]
	logInfo('Poll OpenWeatherMap.org: ' + redactApiKey(ParamsOWM.toString()))
	asynchttpGet('pollOWMHandler', ParamsOWM)
}

void pollOWMHandler(resp, data) {
	if(ifreInstalled()) { updated(); return }
	if(resp.getStatus() != 200 && resp.getStatus() != 207) {
		logWarn('Calling ' + redactApiKey('https://api.openweathermap.org/data/' + (apiVer==true ? '3.0' : '2.5') + '/onecall?lat=' + (String)altLat + '&lon=' + (String)altLon + '&exclude=minutely,hourly&mode=json&units=imperial&appid=' + apiKey))
		logWarn(resp.getStatus() + sCOLON + resp.getErrorMessage())
	}else{
		Map owm
		try {
			owm = parseJson(resp.data)
		} catch (Exception e) {
			logError('pollOWMHandler: failed to parse JSON response from OpenWeatherMap, skipping this poll: ' + e.message)
			return
		}
		state.owm = owm.toString()
		logInfo('OpenWeatherMap Data: ' + owm.toString())
		if(owm.toString()==sNULL) {
			pauseExecution(1000)
			pollOWM()
			return
		}
// <<<<<<<<<< Begin Setup Global Variables >>>>>>>>>>
        TimeZone tZ= TimeZone.getDefault()
		Date fotime = new Date((Long)owm.current.dt * 1000L)
		state.fotime = fotime.toString()
		Date futime = new Date()
		state.futime = futime.toString()
		state[sSUMLST] = futime.format(state.timeFormat as String, tZ).toString()
		state.Summary_last_poll_date = futime.format(state.dateFormat as String, tZ).toString()

		state.currDate = new Date().format('yyyy-MM-dd', tZ)
		state.currTime = new Date().format('HH:mm', tZ)
		if(state.riseTime <= state.currTime && state.setTime >= state.currTime) {
			state.is_day = sTRU
		}else{
			state.is_day = sFLS
		}
		if(state.currTime < state.tw_begin || state.currTime > state.tw_end) {
			state.is_light = sFLS
		}else{
			state.is_light = sTRU
		}
		if(state.is_light != state.is_lightOld) {
			if(state.is_light==sTRU) {
				logInfo(' Switching to Daytime schedule.')
			}else{
				logInfo(' Switching to Nighttime schedule.')
			}
			initialize_poll()
			state.is_lightOld = state.is_light
		}
// >>>>>>>>>> End Setup Global Variables <<<<<<<<<<

// <<<<<<<<<< Begin Setup Forecast Variables >>>>>>>>>>
		Integer mult_twd = state.mult_twd==sNULL ? 1 : (state.mult_twd as String).toInteger()
		Integer mult_p = state.mult_p==sNULL ? 1 : (state.mult_p as String).toInteger()
		Integer mult_r = state.mult_r==sNULL ? 1 : (state.mult_r as String).toInteger()
		String ddisp_twd = state.ddisp_twd==sNULL ? '%3.0f' : state.ddisp_twd
		String ddisp_p = state.ddisp_p==sNULL ? '%4.0f' : state.ddisp_p
		String ddisp_r = state.ddisp_r==sNULL ? '%2.0f' : state.ddisp_r

		Integer cloudCover
		 if (owm?.current?.clouds==null) {
			 cloudCover = 1
		 }else{
			 cloudCover = (owm.current.clouds <= 1) ? 1 : owm.current.clouds
		 }
		state.cloud = cloudCover.toString()
		state.vis = (state[sDMETR]!='MPH' ? Math.round(owm?.current?.visibility==null ? 0.01 : owm.current.visibility.toBigDecimal() * 0.001 * (state.mult_twd as String).toInteger()) / (state.mult_twd as String).toInteger() : Math.round(owm?.current?.visibility==null ? 0.00 : owm.current.visibility.toBigDecimal() * 0.0006213712 * (state.mult_twd as String).toInteger()) / (state.mult_twd as String).toInteger()).toString()

		List owmCweat
        owmCweat = owm?.current?.weather
		state.condition_id = owmCweat==null || owmCweat[0]?.id==null ? '999' : owmCweat[0].id.toString()
		state.condition_code = getCondCode((state.condition_id as String).toInteger(), state.is_day as String)
		state.OWN_icon = owmCweat == null || owmCweat[0]?.icon==null ? (state.is_day==sTRU ? '50d' : '50n') : owmCweat[0].icon

		List owmDaily
        owmDaily = owm?.daily != null && ((List)owm.daily)[0]?.weather != null ? ((List)owm?.daily)[0].weather : null
		state.forecast_id = owmDaily==null || owmDaily[0]?.id==null ? '999' : owmDaily[0].id.toString()
		state.forecast_code = getCondCode((state.forecast_id as String).toInteger(), sTRU)
		state.forecast_text = owmDaily==null || owmDaily[0]?.description==null ? 'Unknown' : owmDaily[0].description.capitalize()

        state.condition_text = state.iconType== sTRU ? (owmCweat==null || owmCweat[0]?.description==null ? 'Unknown' : owmCweat[0].description.capitalize()): (owm?.daily==null || owm?.daily[0]?.weather[0]?.description==null ? 'Unknown' : owm.daily[0].weather[0].description.capitalize())

		owmDaily = owm?.daily != null ? (List)owm.daily : null
		BigDecimal t_p0 = (!owmDaily[0].rain ? 0 : owmDaily[0].rain.toBigDecimal()) + (!owmDaily[0].snow ? 0 : owmDaily[0].snow.toBigDecimal())
		state.PoP = (!owmDaily[0].pop ? 0 : Math.round(owmDaily[0].pop.toBigDecimal() * 100.toInteger())).toString()
		state.percentPrecip = state.PoP

		Boolean isF = state[sTMETR] == sDF

		if(owmDaily && (threedayTilePublish || precipExtendedPublish || myTilePublish)) {
			BigDecimal t_p1 = (owmDaily==null || !owmDaily[1]?.rain ? 0.00 : owmDaily[1].rain.toBigDecimal()) + (owmDaily==null || !owmDaily[1]?.snow ? 0.00 : owmDaily[1].snow.toBigDecimal())
			BigDecimal t_p2 = (owmDaily==null || !owmDaily[2]?.rain ? 0.00 : owmDaily[2].rain.toBigDecimal()) + (owmDaily==null || !owmDaily[2]?.snow ? 0.00 : owmDaily[2].snow.toBigDecimal())
			state.Precip0 = (Math.max(state.rainToday!=null ? (state.rainToday as String).toBigDecimal() : 0.0,Math.round((state[sRMETR] == 'in' ? t_p0 * 0.03937008 : t_p0) * mult_r) / mult_r)).toString()
			state.Precip1 = (Math.round((state[sRMETR] == 'in' ? t_p1 * 0.03937008 : t_p1) * mult_r) / mult_r).toString()
			state.Precip2 = (Math.round((state[sRMETR] == 'in' ? t_p2 * 0.03937008 : t_p2) * mult_r) / mult_r).toString()
			state.PoP1 = (!owmDaily[1].pop ? 0 : Math.round(owmDaily[1].pop.toBigDecimal() * 100.toInteger())).toString()
			state.PoP2 = (!owmDaily[2].pop ? 0 : Math.round(owmDaily[2].pop.toBigDecimal() * 100.toInteger())).toString()
		}

		if(owmDaily && cloudExtendedPublish) {
			state.Cloud0 = (owmDaily[0].clouds==null ? 1 : owmDaily[0].clouds <= 1 ? 1 : owmDaily[0].clouds).toString()
			state.Cloud1 = (owmDaily[1].clouds==null ? 1 : owmDaily[1].clouds <= 1 ? 1 : owmDaily[1].clouds).toString()
			state.Cloud2 = (owmDaily[2].clouds==null ? 1 : owmDaily[2].clouds <= 1 ? 1 : owmDaily[2].clouds).toString()
		}

		if(moonPhaseDetailPublish) {
			calcMoonPhaseValue(owmDaily && owmDaily.size() > 0 ? owm.daily[0] : [:], owmDaily && owmDaily.size() > 1 ? owm.daily[1] : [:], owmDaily && owmDaily.size() > 2 ? owm.daily[2] : [:])
		}

		String imgT1=((state[sICON] as String).toLowerCase().contains('://github.com/') && (state[sICON] as String).toLowerCase().contains('/blob/master/') ? '?raw=true' : sBLK)
		if(owmDaily && owmDaily[1] && owmDaily[2]) {
            String tmpImg0= state[sICON] + (state.iconType== sTRU ? getImgName((state.condition_id as String).toInteger(), state.is_day as String) : getImgName((state.forecast_id as String).toInteger(), state.is_day as String)) + imgT1
			String stockOrAlt1 = altIconsEnable==false ? owmDaily[1].weather[0]?.icon ? "https://openweathermap.org/img/wn/${owmDaily[1].weather[0].icon}.png" : (state[sICON] + getImgName((!owmDaily[1].weather[0].id ? 999 : owmDaily[1].weather[0].id.toInteger()), sTRU) + imgT1) : (state[sICON] + getImgName((!owmDaily[1].weather[0].id ? 999 : owmDaily[1].weather[0].id.toInteger()), sTRU) + imgT1)
			String stockOrAlt2 = altIconsEnable==false ? owmDaily[2].weather[0]?.icon ? "https://openweathermap.org/img/wn/${owmDaily[2].weather[0].icon}.png" : (state[sICON] + getImgName((!owmDaily[2].weather[0].id ? 999 : owmDaily[2].weather[0].id.toInteger()), sTRU) + imgT1) : (state[sICON] + getImgName((!owmDaily[2].weather[0].id ? 999 : owmDaily[2].weather[0].id.toInteger()), sTRU) + imgT1)
			String tmpImg1= stockOrAlt1
			String tmpImg2= stockOrAlt2

			if(threedayTilePublish || myTilePublish || fcstHighLowPublish) {
				state.day1 = owmDaily[1]?.dt==null ? sBLK : new Date((Long)owmDaily[1].dt * 1000L).format('EEEE')
				state.day2 = owmDaily[2]?.dt==null ? sBLK : new Date((Long)owmDaily[2].dt * 1000L).format('EEEE')
				state.is_day1 = sTRU
				state.is_day2 = sTRU
				state.forecast_id1 = owmDaily[1]?.weather[0]?.id==null ? '999' : owmDaily[1].weather[0].id.toString()
				state.forecast_code1 = getCondCode((state.forecast_id1 as String).toInteger(), sTRU)
				state.forecast_text1 = owmDaily[1]?.weather[0]?.description==null ? 'Unknown' : owmDaily[1].weather[0].description.capitalize()

				state.forecast_id2 = owmDaily[2]?.weather[0]?.id==null ? '999' : owmDaily[2].weather[0].id.toString()
				state.forecast_code2 = getCondCode((state.forecast_id2 as String).toInteger(), sTRU)
				state.forecast_text2 = owmDaily[2]?.weather[0]?.description==null ? 'Unknown' : owmDaily[2].weather[0].description.capitalize()

				state.forecastHigh1 = adjTemp(owmDaily[1]?.temp?.max, isF, mult_twd).toString()
				state.forecastHigh2 = adjTemp(owmDaily[2]?.temp?.max, isF, mult_twd).toString()

				state.forecastLow1 = adjTemp(owmDaily[1]?.temp?.min, isF, mult_twd).toString()
				state.forecastLow2 = adjTemp(owmDaily[2]?.temp?.min, isF, mult_twd).toString()
				state.forecastMorn = adjTemp(owmDaily[0]?.temp?.morn, isF, mult_twd).toString()
				state.forecastDay = adjTemp(owmDaily[0]?.temp?.day, isF, mult_twd).toString()
				state.forecastEve = adjTemp(owmDaily[0]?.temp?.eve, isF, mult_twd).toString()
				state.forecastNight = adjTemp(owmDaily[0]?.temp?.night, isF, mult_twd).toString()

				state.forecastMorn1 = adjTemp(owmDaily[1]?.temp?.morn, isF, mult_twd).toString()
				state.forecastDay1 = adjTemp(owmDaily[1]?.temp?.day, isF, mult_twd).toString()
				state.forecastEve1 = adjTemp(owmDaily[1]?.temp?.eve, isF, mult_twd).toString()
				state.forecastNight1 = adjTemp(owmDaily[1]?.temp?.night, isF, mult_twd).toString()

				state.imgName0 = sIMGS5 + tmpImg0 + sRB // For the daily forecast text for 'Toady'
				state.imgName1 = sIMGS5 + tmpImg1 + sRB
				state.imgName2 = sIMGS5 + tmpImg2 + sRB
			}
			if(condition_icon_urlPublish) {
				sendIfChanged(name: 'condition_icon_url1', value: tmpImg1)
				sendIfChanged(name: 'condition_icon_url2', value: tmpImg2)
            }else{
				device.deleteCurrentState('condition_icon_url1')
				device.deleteCurrentState('condition_icon_url2')
			}
		}

		state.forecastHigh = adjTemp(owmDaily[0]?.temp?.max, isF, mult_twd).toString()
		state.forecastLow = adjTemp(owmDaily[0]?.temp?.min, isF, mult_twd).toString()

		if(precipExtendedPublish){
			state.rainTomorrow = state.Precip1
			state.rainDayAfterTomorrow = state.Precip2
        }else{
			device.deleteCurrentState('rainTomorrow')
			device.deleteCurrentState('rainDayAfterTomorrow')
		}
		if(cloudExtendedPublish){
			state.cloudToday = state.Cloud0
			state.cloudTomorrow = state.Cloud1
			state.cloudDayAfterTomorrow = state.Cloud2
        }else{
            device.deleteCurrentState('cloudToday')
			device.deleteCurrentState('cloudTomorrow')
			device.deleteCurrentState('cloudDayAfterTomorrrow')
		}

// <<<<<<<<<< Begin Process Only If Wind from WD Is NOT Selected  >>>>>>>>>>
		if(sourceWind==false){
			String w_string_bft, w_bft_icon
            w_string_bft=sNULL
			w_bft_icon=sNULL
			BigDecimal t_ws = owm?.current?.wind_speed==null ? 0.00 : owm.current.wind_speed.toBigDecimal()
			if(t_ws < 1.0) {
				w_string_bft = 'Calm'; w_bft_icon = 'wb0.png'
			}else if(t_ws < 4.0) {
				w_string_bft = 'Light air'; w_bft_icon = 'wb1.png'
			}else if(t_ws < 8.0) {
				w_string_bft = 'Light breeze'; w_bft_icon = 'wb2.png'
			}else if(t_ws < 13.0) {
				w_string_bft = 'Gentle breeze'; w_bft_icon = 'wb3.png'
			}else if(t_ws < 19.0) {
				w_string_bft = 'Moderate breeze'; w_bft_icon = 'wb4.png'
			}else if(t_ws < 25.0) {
				w_string_bft = 'Fresh breeze'; w_bft_icon = 'wb5.png'
			}else if(t_ws < 32.0) {
				w_string_bft = 'Strong breeze'; w_bft_icon = 'wb6.png'
			}else if(t_ws < 39.0) {
				w_string_bft = 'High wind, moderate gale, near gale'; w_bft_icon = 'wb7.png'
			}else if(t_ws < 47.0) {
				w_string_bft = 'Gale, fresh gale'; w_bft_icon = 'wb8.png'
			}else if(t_ws < 55.0) {
				w_string_bft = 'Strong/severe gale'; w_bft_icon = 'wb9.png'
			}else if(t_ws < 64.0) {
				w_string_bft = 'Storm, whole gale'; w_bft_icon = 'wb10.png'
			}else if(t_ws < 73.0) {
				w_string_bft = 'Violent storm'; w_bft_icon = 'wb11.png'
			}else if(t_ws >= 73.0) {
				w_string_bft = 'Hurricane force'; w_bft_icon = 'wb12.png'
			}
			state.wind_string_bft = w_string_bft
			state.wind_bft_icon = w_bft_icon

			BigDecimal t_wd = owm?.current?.wind_speed==null ? 0.00 : owm.current.wind_speed.toBigDecimal()
			BigDecimal t_wg = owm?.current?.wind_gust==null ? t_wd : owm.current.wind_gust
			if(state[sDMETR] == 'MPH') {
				t_wd = Math.round(t_wd * mult_twd) / mult_twd
				t_wg = Math.round(t_wg * mult_twd) / mult_twd
			} else if(state[sDMETR] == 'KPH') {
				t_wd = Math.round(t_wd * 1.609344 * mult_twd) / mult_twd
				t_wg = Math.round(t_wg * 1.609344 * mult_twd) / mult_twd
			} else if(state[sDMETR] == 'knots') {
				t_wd = Math.round(t_wd * 0.868976 * mult_twd) / mult_twd
				t_wg = Math.round(t_wg * 0.868976 * mult_twd) / mult_twd
			}else{  //  this leave only m/s
				t_wd = Math.round(t_wd * 0.44704 * mult_twd) / mult_twd
				t_wg = Math.round(t_wg * 0.44704 * mult_twd) / mult_twd
			}
			state.wind = t_wd.toString()
			state.wind_gust = t_wg.toString()

			state.wind_degree = owm.current.wind_deg.toInteger().toString()
			BigDecimal twb = owm?.current?.wind_deg==null ? 0.00 : owm.current.wind_deg.toBigDecimal()
			Map wDir = calcWindDirection(twb)
			state.wind_direction = wDir.full
			state.wind_cardinal = wDir.cardinal
			state.windDirImageUrl = wDir.iconUrl
			state.windDirectionImage = wDir.icon
			state.windDirectionEmojiIcon = wDir.emoji
			state.wind_string = w_string_bft + ' from the ' + state.wind_direction + (myGetDataBD('wind') < 1.0 ? sBLK: ' at ' + String.format(ddisp_twd, myGetDataBD('wind')) + sSPC + state[sDMETR])
		}
// >>>>>>>>>> End Process Only If Wind from WD Is NOT Selected <<<<<<<<<<
// >>>>>>>>>> End Setup Forecast Variables <<<<<<<<<<

//<<<<<<<<< Begin Process Only If Illumination from WD Is NOT Selected  >>>>>>>>>>
		 updateLux(false)
//
// >>>>>>>>>> End Process Only If Illumination from WD Is NOT Selected  <<<<<<<<<<

// <<<<<<<<<< Begin Process Only If Ultraviolet Index from WD Is NOT Selected  >>>>>>>>>>
		if(sourceUV==false){
			state.ultravioletIndex = owm?.current?.uvi==null ? "0.00" : owm.current.uvi.toBigDecimal().toString()
		}
// >>>>>>>>>> End Process Only If Ultraviolet Index from WD Is NOT Selected  <<<<<<<<<<

// <<<<<<<<<< Begin Process Only If feelsLike Index from WD Is NOT Selected  >>>>>>>>>>
		if(sourcefeelsLike==false){
			state.feelsLike = adjTemp(owm?.current?.feels_like, isF, mult_twd).toString()
		}
// >>>>>>>>>> End Process Only If feelsLike from WD Is NOT Selected  <<<<<<<<<<
		if(alertPublish) {
			if(alertSource==sTWO) {
/*  for testing a different Lat/Lon location uncommnent the two lines below */
//	String altLat = "38.627003" //"44.809122" // "40.6" //"30.6953657"
//	String altLon = "-90.199402" //"-68.735892" // "-75.43" //-88.0398912"
				pollWDG()
			}
			if((alertSource==sZERO) || (!owm.alerts && alertSource==sONE) || (state.curAl==sNCWA && alertSource==sTWO)) {
				clearAlerts()
			}else{
				if(alertSource==sONE) {
					Map owmAlerts0= owm?.alerts ? owm.alerts[0] : null
					String curAl = owmAlerts0?.event==null ? sNCWA : owmAlerts0.event.replaceAll('\n', sSPC).replaceAll('[{}\\[\\]]', sBLK)
					String curAlSender = owmAlerts0?.sender_name==null ? sNULL : owmAlerts0.sender_name.replaceAll('\n',sSPC).replaceAll('[{}\\[\\]]', sBLK)
					String curAlDescr = owmAlerts0?.description==null ? sNULL : owmAlerts0.description.replaceAll('\n',sSPC).replaceAll('[{}\\[\\]]', sBLK).take(1024)
					if(curAl==sNCWA) {
						clearAlerts()
					}else{
						Integer alertCnt; alertCnt = 0
						Integer i
						for(i = 1;i<10;i++) {
                            if(owm?.alerts[i]?.event!=null) {
								alertCnt++
							}
						}
						state.alertCnt = alertCnt.toString()
					}
					state.alert = curAl + (state.alertCnt != sZERO ? ' +' + state.alertCnt : sBLK)
					state.curAlSender = curAlSender
					state.curAlDescr = curAlDescr
					logInfo('OWM Weather Alert: ' + curAl + '; Description: ' + curAlDescr.length() + ' ' +curAlDescr)
					state.alertTileLink = '<a style="font-style:italic;color:red" href="https://openweathermap.org/city/' + state.OWML + '" target="_blank">'+state.alert+sACB
					state.alertLink = '<a style="font-style:italic;color:red">'+state.alert+sACB
				}else{
/*  for testing a different Lat/Lon location uncommnent the two lines below */
//	String altLat = "38.627003" //"44.809122" // "40.6" //"30.6953657"
//	String altLon = "-90.199402" //"-68.735892" // "-75.43" //-88.0398912"
					state.alert = state.curAl + (state.alertCnt != sZERO ? ' +' + state.alertCnt : sBLK)
// https://tinyurl.com/zznws points to https://forecast.weather.gov/MapClick.php
					state.alertTileLink = '<a style="font-style:italic;color:red" href="https://tinyurl.com/zznws?lat=' + altLat + '&lon=' + altLon + '" target=\'_blank\'>'+state.alert+sACB
					state.alertLink = '<a style="font-style:italic;color:red">'+state.alert+sACB
					if(state.curAl==sNCWA) {
						clearAlerts()
					}
				}
				state.noAlert = sFLS
				state.alertDescr = state.curAlDescr
				state.alertSender = state.curAlSender
				state.possAlert = sTRU
			}
			//  <<<<<<<<<< Begin Built alertTile >>>>>>>>>>
			String alertTile
            alertTile = (state.alert== sNCWA ? 'No Weather Alerts for ' : 'Weather Alert for ') + state.city + (state.alertSender==null || state.alertSender==sSPC ? '' : ' issued by ' + state.alertSender) + sBR
			alertTile+= state.alertTileLink + sBR
			if(alertSource==sONE) {
				alertTile+= '<a href="https://openweathermap.org/city/' + state.OWML + '" target="_blank">' + sIMGS5 + state[sICON] + 'OWM.png style="height:2em"></a> @ ' + state[sSUMLST]
			}else{
				if(alertSource==sTWO) {
    				alertTile+= '<a href=https://tinyurl.com/zznws?lat=' + altLat + '&lon=' + altLon + '" target="_blank">' + sIMGS5 + state[sICON] + 'NWS_240px.png style="height:2em"></a> @ ' + state[sSUMLST]
				}
			}
			state.alertTile = alertTile
			sendIfChanged(name: 'alert', value: state.alert)
			sendIfChanged(name: 'alertDescr', value: state.alertDescr)
			sendIfChanged(name: 'alertSender', value: state.alertSender)
			sendIfChanged(name: 'alertTile', value: state.alertTile)
			//  >>>>>>>>>> End Built alertTile <<<<<<<<<<
		}
		// <<<<<<<<<< Begin Icon Processing  >>>>>>>>>>
		String imgName = (state.iconType== sTRU ? getImgName((state.condition_id as String).toInteger(), state.is_day as String) : getImgName((state.forecast_id as String).toInteger(), state.is_day as String))
        sendIfChangedPublish(name: 'condition_icon', value: sIMGS5 + state[sICON] + imgName + imgT1 + sRB)
		sendIfChangedPublish(name: 'condition_iconWithText', value: sIMGS5 + state[sICON] + imgName + imgT1 + sRB+ sBR + state.condition_text)
		sendIfChangedPublish(name: 'condition_icon_url', value: state[sICON] + imgName + imgT1)
		state.condition_icon_url = state[sICON] + imgName + imgT1
		sendIfChangedPublish(name: 'condition_icon_only', value: imgName.split('/')[-1].replaceFirst('\\?raw=true',sBLK))
		// >>>>>>>>>> End Icon Processing <<<<<<<<<<
		if(state.forecastPoll == sFLS){
			state.forecastPoll = sTRU
		}
		PostPoll()
		return
	}
}
// >>>>>>>>>> End OpenWeatherMap Poll Routines <<<<<<<<<<
// <<<<<<<<<< Begin polling weather.gov for Alerts >>>>>>>>>>
void pollWDG() {
/*  for testing a different Lat/Lon location uncommnent the two lines below */
//	String altLat = "38.627003" //"44.809122" // "40.6" //"30.6953657"
//	String altLon = "-90.199402" //"-68.735892" // "-75.43" //-88.0398912"
	Map wdgParams = [ uri: 'https://api.weather.gov/alerts/active?status=actual&message_type=alert,update&point=' + altLat + ',' + altLon,
		requestContentType:'application/json',
		contentType:'application/json',
		timeout: 20
	]
	logInfo('Poll api.weather.gov/alerts/active: ' + wdgParams)
	asynchttpGet('pollWDGHandler', wdgParams)
}

void pollWDGHandler(resp, data) {
	logInfo('Polling weather.gov')
	if(resp.getStatus() != 200 && resp.getStatus() != 207) {
		logWarn('Calling https://api.weather.gov/alerts/active?status=actual&message_type=alert,update&point=' + altLat + ',' + altLon)
		logWarn(resp.getStatus() + sCOLON + resp.getErrorMessage())
	}else{
		Map wdg
		try {
			wdg = parseJson(resp.data)
		} catch (Exception e) {
			logError('pollWDGHandler: failed to parse JSON response from weather.gov, skipping this poll: ' + e.message)
			return
		}
		state.wdg = wdg.toString()
		logInfo('weather.gov Data: ' + wdg.toString())
		if(wdg.toString()==sNULL) {
			pauseExecution(1000)
			pollWDG()
			return
		}
		state.curAl = wdg?.features[0]?.properties?.event == null ? sNCWA : wdg.features[0].properties.event.replaceAll('\n', sSPC).replaceAll('[{}\\[\\]]', sBLK)
		state.curAlSender = wdg?.features[0]?.properties?.senderName==null ? sNULL : ((String)wdg.features[0].properties.senderName).replaceAll('\n',sSPC).replaceAll('[{}\\[\\]]', sBLK)
		state.curAlDescr = wdg?.features[0]?.properties?.description==null ? sNULL : ((String)wdg.features[0].properties.description).replaceAll('\n',sSPC).replaceAll('[{}\\[\\]]', sBLK).take(1024)
        Integer alertCnt; alertCnt = 0
        Integer i
        for(i = 1;i<10;i++) {
			if(wdg?.features[i]?.properties?.event!=null) {
				alertCnt++
			}
		}
		state.alertCnt = alertCnt.toString()
	}
}
// >>>>>>>>>> End polling weather.gov for Alerts <<<<<<<<<<

static BigDecimal adjTemp(temp, Boolean isF, Integer mult_twd){
	BigDecimal t_fl
	t_fl = temp==null ? 0.00 : temp.toBigDecimal()
	if(!isF) t_fl = (t_fl - 32.0) / 1.8
	t_fl = Math.round(t_fl * mult_twd) / mult_twd
	return t_fl
}

void clearAlerts(){
	state.noAlert = sTRU
	state.alert = 'No current weather alerts for this area'
	state.alertDescr = 'No current weather alerts for this area'
	state.alertSender = sSPC
	String al3 = '<a style="font-style:italic">'
	state.alertTileLink = al3+state.alert+sACB
	state.alertLink = sAB + state.condition_text + sACB
	state.possAlert = sFLS
}

// Legacy accessor kept only for the BigDecimal-parsing convenience it provided; now reads Hubitat's
// native per-device state map directly instead of the old static, cross-device dataStoreFLD map.
BigDecimal myGetDataBD(String key){
	def val = state[key]
	return (val != null && val != sNULL) ? (val as String).toBigDecimal() : 0.0
}

// >>>>>>>>>> Begin Lux Processing <<<<<<<<<<
void updateLux(Boolean pollAgain=true) {
	logInfo('Calling UpdateLux(' + pollAgain + ')')
	if(pollAgain) {
		String curTime = new Date().format('HH:mm', TimeZone.getDefault())
		String newLight
		if(curTime < state.tw_begin || curTime > state.tw_end) {
			newLight =  sFLS
		}else{
			newLight =  sTRU
		}
		if(newLight != state.is_lightOld || state.condition_id==sNULL || state.cloud==sNULL) {
			if(extSource.toInteger()==1 || sourceIllumination == true){
				pollWD()
				return
			}else{
				pollOWM()
				return
			}
		}
	}
	if(extSource.toInteger()==1 || sourceIllumination == true){
		def (Long lux, String bwn) = estimateLux((state.condition_id as String).toInteger(), (state.cloud as String).toInteger())
		state.bwn = bwn
		logInfo('updateLux Results: lux: ' + lux + '; bwn: ' + bwn)
	}else{
		def (lux, bwn) = estimateLux((state.condition_id as String).toInteger(), (state.cloud as String).toInteger())
		state.illuminance = !lux ? sZERO : lux.toString()
		state.illuminated = String.format('%,4d', !lux ? 0 : lux).toString()
		state.bwn = bwn
		logInfo('updateLux Results: lux: ' + lux + '; bwn: ' + bwn)
	}
	if(pollAgain) PostPoll()
	return
}
// >>>>>>>>>> End Lux Processing <<<<<<<<<<

// <<<<<<<<<< Begin Post-Poll Routines >>>>>>>>>>
void PostPoll() {
	if(ifreInstalled()) { updated(); return }
    String tfmt='yyyy-MM-dd\'T\'HH:mm:ssXXX'
	String tfmt1=state.timeFormat
    String dfmt1=state.dateFormat
	String tfmt2='EEE MMM dd HH:mm:ss z yyyy'
    TimeZone tZ= TimeZone.getDefault()
    if(localSunrisePublish){  // don't bother setting these values if it's not enabled
        sendIfChanged(name: 'tw_begin', value: state.tw_begin)
        sendIfChanged(name: 'sunriseTime', value: state.riseTime)
        sendIfChanged(name: 'noonTime', value: state.noonTime)
        sendIfChanged(name: 'sunsetTime', value: state.setTime)
        sendIfChanged(name: 'tw_end', value: state.tw_end)
    }else{
        device.deleteCurrentState('tw_begin')
        device.deleteCurrentState('sunriseTime')
        device.deleteCurrentState('noonTime')
        device.deleteCurrentState('sunsetTime')
        device.deleteCurrentState('tw_end')
    }
    if(dashSharpToolsPublish || dashSmartTilesPublish || localSunrisePublish) {
        sendIfChanged(name: 'localSunset', value: state.localSunset) // only needed for certain dashboards
        sendIfChanged(name: 'localSunrise', value: state.localSunrise) // only needed for certain dashboards
    }else{
        device.deleteCurrentState('localSunset')
        device.deleteCurrentState('localSunrise')
    }

	Integer mult_twd = state.mult_twd==sNULL ? 1 : (state.mult_twd as String).toInteger()
	Integer mult_p = state.mult_p==sNULL ? 1 : (state.mult_p as String).toInteger()
	Integer mult_r = state.mult_r==sNULL ? 1 : (state.mult_r as String).toInteger()
	String ddisp_twd = state.ddisp_twd==sNULL ? '%3.0f' : state.ddisp_twd
	String ddisp_p = state.ddisp_p==sNULL ? '%4.0f' : state.ddisp_p
	String ddisp_r = state.ddisp_r==sNULL ? '%2.0f' : state.ddisp_r

/*  Weather-Display Data Elements */
	sendIfChanged(name: 'humidity', value: state.humidity==sNULL ? 0 : myGetDataBD('humidity'), unit: '%')
	sendIfChanged(name: 'illuminance', value: state.illuminance==sNULL ? 5 : (state.illuminance as String).toInteger(), unit: 'lx')
	sendIfChanged(name: 'pressure', value: state.pressure==sNULL ? 0 : Math.round(myGetDataBD('pressure') * mult_p) / mult_p, unit: state[sPMETR])
    if(dashSharpToolsPublish || dashSmartTilesPublish) {
        sendIfChanged(name: 'pressured', value: String.format(ddisp_p, myGetDataBD('pressure')), unit: state[sPMETR])
    }else{
		device.deleteCurrentState('pressured')
	}
	String tmetr= state[sTMETR]
	sendIfChanged(name: sTEMP, value: myGetDataBD(sTEMP), unit: tmetr)
	sendIfChanged(name: 'ultravioletIndex', value: myGetDataBD('ultravioletIndex'), unit: 'uvi')
	sendIfChanged(name: 'feelsLike', value: myGetDataBD('feelsLike'), unit: tmetr)

/*  'Required for Dashboards' Data Elements */
	if(dashHubitatOWMPublish || dashSharpToolsPublish || dashSmartTilesPublish) {
        sendIfChanged(name: 'city', value: state.city)
    }else{
        device.deleteCurrentState('city')
	}
	if(dashSharpToolsPublish) {
        sendIfChanged(name: 'forecastIcon', value: getCondCode((state.condition_id as String).toInteger(), state.is_day as String))
    }else{
     device.deleteCurrentState('forecastIcon')
	}
	if(dashSharpToolsPublish || dashSmartTilesPublish || rainTodayPublish) {
        sendIfChanged(name: 'rainToday', value: state.rainToday==sNULL ? 0 : Math.round((state.rainToday as String).toBigDecimal() * mult_r) / mult_r, unit: state[sRMETR])
    }else{
        device.deleteCurrentState('rainToday')
	}
	if(dashSharpToolsPublish || dashSmartTilesPublish || percentPrecipPublish) {
        sendIfChanged(name: 'percentPrecip', value: (state.percentPrecip as String).toInteger())
    }else{
        device.deleteCurrentState('percentPrecip')
	}
	if(dashSharpToolsPublish || dashSmartTilesPublish) {
        sendIfChanged(name: 'weather', value: state.condition_text)
    }else{
        device.deleteCurrentState('weather')
	}
	if(dashSharpToolsPublish || dashSmartTilesPublish) {
        sendIfChanged(name: 'weatherIcon', value: getCondCode((state.condition_id as String).toInteger(), state.is_day as String))
    }else{
        device.deleteCurrentState('weatherIcon')
	}
	if(dashHubitatOWMPublish) {
        sendIfChanged(name: "weatherIcons", value: state.OWN_icon)
    }else{
        device.deleteCurrentState('weatherIcons')
	}
	if(dashHubitatOWMPublish || dashSharpToolsPublish || windPublish) {
        sendIfChanged(name: 'wind', value: state.wind==sNULL ? 0 : Math.round(myGetDataBD('wind') * mult_twd) / mult_twd, unit: state[sDMETR])
    }else{
        device.deleteCurrentState('wind')
	}
	if(dashHubitatOWMPublish) {
        sendIfChanged(name: 'windSpeed', value: state.wind==sNULL ? 0 : Math.round(myGetDataBD('wind') * mult_twd) / mult_twd, unit: state[sDMETR])
    }else{
        device.deleteCurrentState('windSpeed')
	}
	if(dashHubitatOWMPublish) {
        sendIfChanged(name: 'windDirection', value: state.wind_degree==sNULL ? 0 : (state.wind_degree as String).toInteger(), unit: 'DEGREE')
    }else{
        device.deleteCurrentState('windDirection')
	}

/*  Selected optional Data Elements */
	sendIfChangedPublish(name: 'betwixt', value: state.bwn)
	sendIfChangedPublish(name: 'cloud', value: (state.cloud as String).toInteger(), unit: '%')
	sendIfChangedPublish(name: 'condition_code', value: state.condition_code)
	sendIfChangedPublish(name: 'condition_text', value: state.condition_text)
	sendIfChangedPublish(name: 'dewpoint', value: state.dewpoint==sNULL ? 0 : myGetDataBD('dewpoint'), unit: tmetr)

	sendIfChangedPublish(name: 'forecast_code', value: state.forecast_code)
	if(forecast_textPublish) {
		sendIfChangedPublish(name: 'forecast_text', value: state.forecast_text)
		sendIfChanged(name: 'forecast_text1', value: state.forecast_text1)
		sendIfChanged(name: 'forecast_text2', value: state.forecast_text2)
    }else{
        device.deleteCurrentState('forecast_text1')
		device.deleteCurrentState('forecast_text2')
	}
	if(fcstHighLowPublish){ // don't bother setting these values if it's not enabled
		sendIfChanged(name: 'forecastHigh', value: Math.round((state.forecastHigh as String).toBigDecimal() * mult_twd) / mult_twd, unit: tmetr)
		sendIfChanged(name: 'forecastHigh1', value: Math.round((state.forecastHigh1 as String).toBigDecimal() * mult_twd) / mult_twd, unit: tmetr)
		sendIfChanged(name: 'forecastHigh2', value: Math.round((state.forecastHigh2 as String).toBigDecimal() * mult_twd) / mult_twd, unit: tmetr)
		sendIfChanged(name: 'forecastLow', value: Math.round((state.forecastLow as String).toBigDecimal() * mult_twd) / mult_twd, unit: tmetr)
		sendIfChanged(name: 'forecastLow1', value: Math.round((state.forecastLow1 as String).toBigDecimal() * mult_twd) / mult_twd, unit: tmetr)
		sendIfChanged(name: 'forecastLow2', value: Math.round((state.forecastLow2 as String).toBigDecimal() * mult_twd) / mult_twd, unit: tmetr)
		sendIfChanged(name: 'forecastMorn', value: (state.forecastMorn as String).toBigDecimal(), unit: tmetr)
		sendIfChanged(name: 'forecastDay', value: (state.forecastDay as String).toBigDecimal(), unit: tmetr)
		sendIfChanged(name: 'forecastEve', value: (state.forecastEve as String).toBigDecimal(), unit: tmetr)
		sendIfChanged(name: 'forecastNight', value: (state.forecastNight as String).toBigDecimal(), unit: tmetr)
		sendIfChanged(name: 'forecastMorn1', value: (state.forecastMorn1 as String).toBigDecimal(), unit: tmetr)
		sendIfChanged(name: 'forecastDay1', value: (state.forecastDay1 as String).toBigDecimal(), unit: tmetr)
		sendIfChanged(name: 'forecastEve1', value: (state.forecastEve1 as String).toBigDecimal(), unit: tmetr)
		sendIfChanged(name: 'forecastNight1', value: (state.forecastNight1 as String).toBigDecimal(), unit: tmetr)
    }else{
        device.deleteCurrentState('forecastHigh')
		device.deleteCurrentState('forecastHigh1')
		device.deleteCurrentState('forecastHigh2')
		device.deleteCurrentState('forecastLow')
		device.deleteCurrentState('forecastLow1')
		device.deleteCurrentState('forecastLow2')
		device.deleteCurrentState('forecastMorn')
		device.deleteCurrentState('forecastDay')
		device.deleteCurrentState('forecastEve')
		device.deleteCurrentState('forecastNight')
		device.deleteCurrentState('forecastMorn1')
		device.deleteCurrentState('forecastDay1')
		device.deleteCurrentState('forecastEve1')
		device.deleteCurrentState('forecastNight1')
	}
	sendIfChangedPublish(name: 'illuminated', value: state.illuminated + ' lx')
	sendIfChangedPublish(name: 'is_day', value: state.is_day)

//suncalc / sunMoonAngles - both are fed by the single calcSunPosition()/calcMoonPosition() algorithm
	if(suncalcPublish || sunMoonAnglesPublish){
		calcSunPosition()
	}
	if(sunMoonAnglesPublish){
		calcMoonPosition()
	}

//suncalc
	if(suncalcPublish){  // don't bother setting these values if it's not enabled
		sendIfChanged(name: 'altitude', value: state.altitude)
		sendIfChanged(name: 'azimuth', value: state.azimuth)
	}else{
		device.deleteCurrentState('altitude')
		device.deleteCurrentState('azimuth')
	}

//sunMoonAngles (new)
	if(sunMoonAnglesPublish){
		sendIfChanged(name: 'currentSunAltitude', value: state.currentSunAltitude)
		sendIfChanged(name: 'currentSunAzimuth', value: state.currentSunAzimuth)
		sendIfChanged(name: 'currentSunAltitudeText', value: state.currentSunAltitude != null ? "${state.currentSunAltitude}°" : "--°")
		sendIfChanged(name: 'currentSunAzimuthText', value: state.currentSunAzimuth != null ? "${state.currentSunAzimuth}°" : "--°")
		sendIfChanged(name: 'currentMoonAltitude', value: state.currentMoonAltitude)
		sendIfChanged(name: 'currentMoonAzimuth', value: state.currentMoonAzimuth)
		sendIfChanged(name: 'currentMoonAltitudeText', value: state.currentMoonAltitude != null ? "${state.currentMoonAltitude}°" : "--°")
		sendIfChanged(name: 'currentMoonAzimuthText', value: state.currentMoonAzimuth != null ? "${state.currentMoonAzimuth}°" : "--°")
	}else{
		['currentSunAltitude','currentSunAzimuth','currentSunAltitudeText','currentSunAzimuthText','currentMoonAltitude','currentMoonAzimuth','currentMoonAltitudeText','currentMoonAzimuthText'].each { device.deleteCurrentState(it) }
	}

//windDirImage (new)
	if(windDirImagePublish){
		sendIfChanged(name: 'windDirImageUrl', value: state.windDirImageUrl)
		sendIfChanged(name: 'windDirectionImage', value: state.windDirectionImage)
		sendIfChanged(name: 'windDirectionEmojiIcon', value: state.windDirectionEmojiIcon)
	}else{
		['windDirImageUrl','windDirectionImage','windDirectionEmojiIcon'].each { device.deleteCurrentState(it) }
	}

	if(obspollPublish){  // don't bother setting these values if it's not enabled
        sendIfChanged(name: 'last_poll_Forecast', value: new Date().parse(tfmt2, state.futime as String).format(dfmt1, tZ) + ', ' + new Date().parse(tfmt2, state.futime as String).format(tfmt1, tZ))
		sendIfChanged(name: 'last_observation_Forecast', value: new Date().parse(tfmt2, state.fotime as String).format(dfmt1, tZ) + ', ' + new Date().parse(tfmt2, state.fotime as String).format(tfmt1, tZ))
    }else{
        device.deleteCurrentState('last_poll_Forecast')
		device.deleteCurrentState('last_observation_Forecast')
	}

	if(precipExtendedPublish){ // don't bother setting these values if it's not enabled
		sendIfChanged(name: 'rainDayAfterTomorrow', value: Math.round(myGetDataBD('rainDayAfterTomorrow') * mult_r) / mult_r, unit: state[sRMETR])
		sendIfChanged(name: 'rainTomorrow', value: Math.round(myGetDataBD('rainTomorrow') * mult_r) / mult_r, unit: state[sRMETR])
        sendIfChanged(name: 'PoP1', value: (state.PoP1 as String).toInteger())
        sendIfChanged(name: 'PoP2', value: (state.PoP2 as String).toInteger())
    }else{
		device.deleteCurrentState('rainTomorrow')
		device.deleteCurrentState('rainDayAfterTomorrow')
		device.deleteCurrentState('PoP1')
		device.deleteCurrentState('PoP2')
	}
	if(cloudExtendedPublish){ // don't bother setting these values if it's not enabled
		sendIfChanged(name: 'cloudToday', value: (state.cloudToday as String).toInteger(), unit: '%')
		sendIfChanged(name: 'cloudTomorrow', value: (state.cloudTomorrow as String).toInteger(), unit: '%')
		sendIfChanged(name: 'cloudDayAfterTomorrow', value: (state.cloudDayAfterTomorrow as String).toInteger(), unit: '%')
    }else{
		device.deleteCurrentState('cloudToday')
		device.deleteCurrentState('cloudTomorrow')
		device.deleteCurrentState('cloudDayAfterTomorrrow')
    }
	sendIfChangedPublish(name: 'solarradiation', value: state.solarradiation)
	sendIfChangedPublish(name: 'state', value: state.state)
	sendIfChangedPublish(name: 'vis', value: state.vis==sNULL ? 0 : Math.round((state.vis as String).toBigDecimal() * mult_twd) / mult_twd, unit: (state[sDMETR]=='MPH' ? 'miles' : 'kilometers'))
	sendIfChangedPublish(name: 'wind_degree', value: state.wind_degree==sNULL ? 0 : (state.wind_degree as String).toInteger(), unit: 'DEGREE')
	sendIfChangedPublish(name: 'wind_direction', value: state.wind_direction==sNULL ? 'North' : state.wind_direction)
	sendIfChangedPublish(name: 'wind_cardinal', value: state.wind_cardinal==sNULL ? sZERO : state.wind_cardinal)
	sendIfChangedPublish(name: 'wind_gust', value: state.wind_gust==sNULL ? 0 : Math.round(myGetDataBD('wind_gust') * mult_twd) / mult_twd, unit: state[sDMETR])
	sendIfChangedPublish(name: 'wind_string', value: state.wind_string==sNULL ? sBLK : state.wind_string)

//moonPhaseDetail (new - if new OWM daily data wasn't available this poll, keep whatever was last published)
	if(!moonPhaseDetailPublish){
		['todayMoonPhase','tomMoonPhase','tdaMoonPhase','todayMoonPhaseText','tomMoonPhaseText','tdaMoonPhaseText','todayMoonPhasePngImageUrl','tomMoonPhasePngImageUrl','tdaMoonPhasePngImageUrl','todayMoonPhaseSvgImage','tomMoonPhaseSvgImage','tdaMoonPhaseSvgImage','todayMoonPhaseEmojiIcon','tomMoonPhaseEmojiIcon','tdaMoonPhaseEmojiIcon','currentMoonPhaseTile'].each { device.deleteCurrentState(it) }
	}

	state[sSUMLST] = (state.sutime > state.futime ? new Date().parse(tfmt2, state.sutime as String).format(tfmt1, TimeZone.getDefault()) : new Date().parse(tfmt2, state.futime as String).format(tfmt1, TimeZone.getDefault()))
	state.Summary_last_poll_date = (state.sutime > state.futime ? new Date().parse(tfmt2, state.sutime as String).format(dfmt1, TimeZone.getDefault()) : new Date().parse(tfmt2, state.futime as String).format(dfmt1, TimeZone.getDefault()))
//  <<<<<<<<<< Begin Built Weather Summary text >>>>>>>>>>
	if(weatherSummaryPublish){ // don't bother setting these values if it's not enabled
		String Summary_forecastTemp
		String Summary_precip
		String Summary_vis
		String mtprecip
		if(extSource.toInteger() == 2){
			Summary_forecastTemp = ' with a high of ' + String.format(ddisp_twd, myGetDataBD('forecastHigh')) + tmetr + ' and a low of ' + String.format(ddisp_twd, myGetDataBD('forecastLow')) + tmetr + '. '
			Summary_precip = 'There is a ' + state.percentPrecip + '% chance of precipitation. '
			Summary_vis = 'Visibility is around ' + String.format(ddisp_twd, myGetDataBD('vis')) + (state[sDMETR]=='MPH' ? ' miles.' : ' kilometers.')
		}else{
			Summary_forecastTemp = sBLK
			Summary_precip = sBLK
			Summary_vis = sBLK
			mtprecip = 'N/A'
		}
		SummaryMessage(summaryType, state.Summary_last_poll_date as String, state[sSUMLST] as String, Summary_forecastTemp, Summary_precip, Summary_vis)
	}
//  >>>>>>>>>> End Built Weather Summary text <<<<<<<<<<

	generateTiles(tmetr, ddisp_twd, ddisp_p, ddisp_r, mult_twd)
}

// Consolidated dashboard-tile builder, matching the community OpenWeatherMap Multi-API Weather Driver's
// generateTiles() pattern: every tile is built here (from state, populated earlier in the poll), each
// gated by its existing optional-attribute toggle, with an optional debug length badge.
private void generateTiles(String tmetr, String ddisp_twd, String ddisp_p, String ddisp_r, Integer mult_twd) {
	Closure appendTileDebug = { String bodyHtml ->
		if (settings.debugTileEnable != true) return bodyHtml
		return bodyHtml + "<div style='position:absolute;top:2px;right:4px;font-size:.6em;color:#fff;background:rgba(0,0,0,0.6);padding:1px 4px;border-radius:3px;z-index:99;'>Len:${bodyHtml.length()}</div>"
	}

	String OWMIcon
	String OWMText
	if((alertSource==sZERO) || (alertSource==sONE) || (state.curAl==sNCWA && alertSource==sTWO)) {
		OWMIcon = '<a href="https://openweathermap.org/city/' + state.OWML + '" target="_blank">' + sIMGS5 + state[sICON] + 'OWM.png style="height:2em"></a> @ ' + state[sSUMLST]
		OWMText = '<a href="https://openweathermap.org" target="_blank">OpenWeatherMap.org</a> @ ' + state[sSUMLST]
	}else{
		OWMIcon = '<a href="https://tinyurl.com/zznws?lat=' + altLat + '&lon=' + altLon + '" target="_blank">' + sIMGS5 + state[sICON] + 'NWS_240px.png style="height:2em"></a> @ ' + state[sSUMLST]
		OWMText = '<a href="https://tinyurl.com/zznws?lat=' + altLat + '&lon=' + altLon + '" target="_blank">Weather.gov</a> @ ' + state[sSUMLST]
	}
		//  <<<<<<<<<< Begin Built 3dayfcstTile >>>>>>>>>>
	if(threedayTilePublish) {
		Boolean gitclose = ((state[sICON] as String).toLowerCase().contains('://github.com/')) && ((state[sICON] as String).toLowerCase().contains('/blob/master/'))
		String iconClose = (gitclose ? '?raw=true>' : sRB)
		String my3day
        my3day = '<style type="text/css">.cI{height:45%}.cIb{height:80%}</style>'
		my3day += '<table style="text-align:center;display:inline">'
		my3day += sTR
		my3day += '<B>' + state.city +'</B>'
		my3day += sTD+'Today'
		my3day += sTD + state.day1
		my3day += sTD + state.day2
		my3day += sTR
		my3day += 'Now' + String.format(ddisp_twd, myGetDataBD(sTEMP)) + tmetr + sBR + 'Feels' + String.format(ddisp_twd, myGetDataBD('feelsLike')) + tmetr
		my3day += sTD + state.imgName0
		my3day += sTD + state.imgName1
		my3day += sTD + state.imgName2
		my3day += sTR
		my3day += sTD + state.condition_text
		my3day += sTD + state.forecast_text1
		my3day += sTD + state.forecast_text2
		my3day += sTR
		if(state.threedayLH==sFLS){
			my3day += 'Low High'
			my3day += sTD + String.format(ddisp_twd, myGetDataBD('forecastLow')) + tmetr + sSPC + String.format(ddisp_twd, myGetDataBD('forecastHigh')) + tmetr
			my3day += sTD + String.format(ddisp_twd, myGetDataBD('forecastLow1')) + tmetr + sSPC + String.format(ddisp_twd, myGetDataBD('forecastHigh1')) + tmetr
			my3day += sTD + String.format(ddisp_twd, myGetDataBD('forecastLow2')) + tmetr + sSPC + String.format(ddisp_twd, myGetDataBD('forecastHigh2')) + tmetr
		}else{
			my3day += 'High Low'
			my3day += sTD + String.format(ddisp_twd, myGetDataBD('forecastHigh')) + tmetr + sSPC + String.format(ddisp_twd, myGetDataBD('forecastLow')) + tmetr
			my3day += sTD + String.format(ddisp_twd, myGetDataBD('forecastHigh1')) + tmetr + sSPC + String.format(ddisp_twd, myGetDataBD('forecastLow1')) + tmetr
			my3day += sTD + String.format(ddisp_twd, myGetDataBD('forecastHigh2')) + tmetr + sSPC + String.format(ddisp_twd, myGetDataBD('forecastLow2')) + tmetr
		}
		my3day += sTR
		my3day += 'PoP Precip'
		my3day += sTD + state.PoP + '% ' + (myGetDataBD('Precip0') > 0 ? String.format(ddisp_r, myGetDataBD('Precip0')) + state[sRMETR] : 'None')
		my3day += sTD + state.PoP1 + '% ' + (myGetDataBD('Precip1') > 0 ? String.format(ddisp_r, myGetDataBD('Precip1')) + state[sRMETR] : 'None')
		my3day += sTD + state.PoP2 + '% ' + (myGetDataBD('Precip2') > 0 ? String.format(ddisp_r, myGetDataBD('Precip2')) + state[sRMETR] : 'None')
		my3day += '<tr style="font-size:85%">' + '<td  colspan="4">'
		my3day += '☀ ' + state.localSunrise + sSPC + '☽ ' + state.localSunset

		if((my3day.length() + OWMIcon.length()+8) < 1025) {
			my3day += OWMIcon
		}else if((my3day.length() + OWMText.length()+8) < 1025) {
			my3day += OWMText
		}else{
			my3day += 'OpenWeatherMap.org'
		}
		my3day += '</table>'
		if(my3day.length() > 1024) {
			logWarn('Too much data to display.</br></br>Current threedayfcstTile length (' + my3day.length() + ') exceeds maximum tile length by ' + (my3day.length() - 1024).toString()  + ' characters.')
		}
		sendIfChanged(name: 'threedayfcstTile', value: appendTileDebug(my3day.take(1024)))
    }else{
		device.deleteCurrentState('threedayfcstTile')
	}
//  >>>>>>>>>> End Built 3dayfcstTile <<<<<<<<<<

	if(myTilePublish) { // don't bother setting these values if it's not enabled
		Boolean gitclose = ((state[sICON] as String).toLowerCase().contains('://github.com/')) && ((state[sICON] as String).toLowerCase().contains('/blob/master/'))
		String iconClose = (gitclose ? '?raw=true>' : sRB)
		Boolean noAlert = (!alertPublish) ? true : (!state.possAlert || state.possAlert == sBLK || state.possAlert == sFLS)
		String alertStyleOpen = (noAlert ? sBLK : '<span>')
		String alertStyleClose = (noAlert ? sBLK : sCSPAN)

		BigDecimal wgust
		if(myGetDataBD('wind_gust') < 1.0 ) {
			wgust = 0.0g
		}else{
			wgust = myGetDataBD('wind_gust')
		}
		String mytext = '<style type="text/css">.cI{height:45%}.cIb{height:80%}</style>'
		mytext += '<table style="text-align:center;display:inline">'
		mytext += sTR + '<B>' + state.city +'</B>'
		mytext += sTR + state.condition_text + (noAlert ? sBLK : ' | ') + alertStyleOpen + (noAlert ? sBLK : state.alertLink) + alertStyleClose
		mytext += sTR + String.format(ddisp_twd, myGetDataBD(sTEMP)) + tmetr  + state.imgName0
		mytext += 'Feels like ' + String.format(ddisp_twd, myGetDataBD('feelsLike')) + tmetr
		mytext += '<tr style="font-size:85%">' + sTD + '༄ ' + state.wind_direction + sSPC
		mytext += (myGetDataBD('wind') < 1.0 ? 'calm' : '@ ' + String.format(ddisp_twd, myGetDataBD('wind')) + sSPC + state[sDMETR])
		mytext += ', gusts ' + ((wgust < 1.0) ? 'calm' :  '@ ' + String.format(ddisp_twd, wgust) + sSPC + state[sDMETR])
		String mytexte = '<tr style="font-size:80%">' +sTD + '⏲ ' + String.format(ddisp_p, myGetDataBD('pressure')) + sSPC + state[sPMETR] + sSPC + '💦 '
   		mytexte += state.humidity + '%' + sSPC + '☂ ' + state.percentPrecip + '%' + sSPC + '🪣 ' + (myGetDataBD('rainToday') > 0 ? String.format(ddisp_r, myGetDataBD('rainToday')) + sSPC + state[sRMETR] : 'None') + sBR
		mytexte += '☀ ' + state.localSunrise + sSPC + '☽ ' + state.localSunset
		if((mytext.length() + mytexte.length() + OWMIcon.length()+8) < 1025) {
			mytext+= mytexte + OWMIcon
		}else{
			mytexte = '<tr style="font-size:80%">' + sTD + '<B>B:</B> ' + String.format(ddisp_p, myGetDataBD('pressure')) + sSPC + state[sPMETR] + sSPC + '<B>H:</B> '
			mytexte += state.humidity + '%' + sSPC + '<B>PoP:</B> ' + state.percentPrecip + '%' + sSPC + '<B>Precip:</B> ' + (myGetDataBD('rainToday') > 0 ? String.format(ddisp_r, myGetDataBD('rainToday')) + sSPC + state[sRMETR] : 'None') + sBR
			mytexte += '<B>SRise:</B> ' + state.localSunrise + sSPC + '<B>SSet:</B> ' + state.localSunset
			mytext+= mytexte
			if((mytext.length() + OWMIcon.length()+8) < 1025) {
				mytext+= OWMIcon
			}else if((mytext.length() + OWMText.length()+8) < 1025) {
				mytext+= OWMText
			}else{
				mytext+= 'OpenWeatherMap.org'
			}
		}
		mytext+= '</table>'
		if(mytext.length() > 1024) {
			logWarn('Too much data to display.</br></br>Current myTile length (' + mytext.length() + ') exceeds maximum tile length by ' + (mytext.length() - 1024).toString()  + ' characters.')
		}
		sendIfChanged(name: 'myTile', value: appendTileDebug(mytext.take(1024)))
    }else{
		device.deleteCurrentState('myTile')
	}
//  >>>>>>>>>> End Built mytext <<<<<<<<<<

//  <<<<<<<<<< Begin Built currentMoonPhaseTile (new) >>>>>>>>>>
	if(moonPhaseDetailPublish) {
		String moonPhaseText = (device.currentValue('todayMoonPhaseText') ?: 'Unknown') as String
		String moonSvg = (device.currentValue('todayMoonPhaseSvgImage') ?: '') as String
		String moonPngUrl = (device.currentValue('todayMoonPhasePngImageUrl') ?: '') as String
		String moonEmoji = (device.currentValue('todayMoonPhaseEmojiIcon') ?: '🌑') as String

		String graphicDisplay
		if (settings.displayTileMoonPhaseSVGEnable == true && moonSvg != "") {
			graphicDisplay = moonSvg
		} else if (moonPngUrl != "") {
			graphicDisplay = "<img src='${moonPngUrl}' style='width:100%;height:100%;object-fit:contain;'>"
		} else {
			graphicDisplay = "<div style='font-size:min(12vw,12vh,4.5em);line-height:1;'>${moonEmoji}</div>"
		}

		String moonBodyHtml = "<div style='display:flex;justify-content:center;align-items:center;height:100%;width:100%;text-align:center;position:relative;overflow:hidden;'>" +
			graphicDisplay +
			"<div style='position:absolute; bottom:10%; width:92%; box-sizing:border-box; text-align:center; text-shadow:1px 1px 3px #000; line-height:1.15; word-wrap:break-word;'>" +
			"<div style='font-size:clamp(.6rem, 6vw, .8rem); font-weight:bold; color:#fff;'>${moonPhaseText}</div>" +
			"</div>" +
			"</div>"
		sendIfChanged(name: 'currentMoonPhaseTile', value: appendTileDebug(moonBodyHtml))
	}
//  >>>>>>>>>> End Built currentMoonPhaseTile <<<<<<<<<<
}
// >>>>>>>>>> End Post-Poll Routines <<<<<<<<<<

void refresh() {
	updateLux(true)
	return
}

void installed() {
}

@Field static Map<String,String> verFLD=[:]

Boolean ifreInstalled(){
	String mc=device.id.toString()
	if(verFLD[mc]!=version()) return true
	return false
}

void updated()   {
	logInfo("running updated()")
	unschedule()
	String mc=device.id.toString()
	verFLD[mc]=version()
	initMe()
	updateCheck()
	runIn(5,'finishSched')
}

Long wnow(){ return (Long)now() }

void finishSched() {
	pollSunRiseSet()
	initialize_poll()
	runEvery10Minutes('updateLux', [Data: [true]])
	Random rand = new Random(wnow())
	Integer ssseconds = rand.nextInt(60)
	schedule("${ssseconds} 20 0/8 ? * * *", 'pollSunRiseSet')
	runIn(5, 'pollData')
	if(settingEnable) runIn(2100,'settingsOff')// 'roll up' (hide) the condition selectors after 35 min
	if(settings.logDebugEnable) runIn(1800,'disableDebugLogging')// turns off debug logging after 30 min
	Integer r_minutes = rand.nextInt(60)
	schedule("0 ${r_minutes} 8 ? * FRI *", 'updateCheck')
}

void disableDebugLogging() {
	logInfo('30 minutes have elapsed. Automatically disabling debug logging.')
	device.updateSetting('logDebugEnable',[value:sFLS,type:'bool'])
}

void initMe() {
	state.is_light = sTRU
	state.is_lightOld = state.is_light //avoid startup oscillation
	String city = (settings.city ?: sBLK)
	state.city = city
	state.threedayLH = settings.threedayLH ? sTRU : sFLS
	Boolean altCoord = (settings.altCoord ?: false)
	String valtLat; valtLat = location.latitude.toString().replace(sSPC, sBLK)
	String valtLon; valtLon = location.longitude.toString().replace(sSPC, sBLK)
	String altLat = settings.altLat ?: valtLat
	String altLon = settings.altLon ?: valtLon
	state.forecastPoll = sFLS

	if (altCoord) {
		if (altLat == null) {
			device.updateSetting('altLat', [value:valtLat,type:'text'])
		}
		if (altLon == null) {
			device.updateSetting('altLon', [value:valtLon,type:'text'])
		}
		if (altLat == null || altLon == null) {
			if ((valtLat == null) || (valtLat = sBLK)) {
				logError('The Override Coordinates feature is selected but Both Hub & the Override Latitude are null.')
			}else{
				device.updateSetting('altLat', [value:valtLat,type:'text'])
			}
			if ((valtLon == null) || (valtLon = sBLK)) {
				logError('The Override Coordinates feature is selected but Both Hub & the Override Longitude are null.')
			}else{
				device.updateSetting('altLon', [value:valtLon,type:'text'])
			}
		}
	}else{
		device.updateSetting('altLat', [value:valtLat,type:'text'])
		device.updateSetting('altLon', [value:valtLon,type:'text'])
		if (altLat == null || altLon == null) {
			if ((valtLat == null) || (valtLat = sBLK)) {
				logError('The Hub\'s latitude is not set. Please set it, or use the Override Coordinates feature.')
			}else{
				device.updateSetting('altLat', [value:valtLat,type:'text'])
			}
			if ((valtLon == null) || (valtLon = sBLK)) {
				logError('The Hub\'s longitude is not set. Please set it, or use the Override Coordinates feature.')
			}else{
				device.updateSetting('altLon', [value:valtLon,type:'text'])
			}
		}
	}

	Boolean iconType = (settings.iconType ?: false)
	state.iconType = iconType ? sTRU : sFLS
//	https://tinyurl.com/icnqz/ points to https://raw.githubusercontent.com/HubitatCommunity/WeatherIcons/master/
	String iconLocation = (settings.iconLocation ?: 'https://tinyurl.com/icnqz/')
	state[sICON] = iconLocation
	state.OWM = '<a href="https://openweathermap.org" target="_blank">' + sIMGS5 + state[sICON] + 'OWM.png style="height:2em"></a>'
	state.moonPhaseImagePath = calcMoonPhaseImagePath(settings.altMoonPhaseImagePath as String)
	state.windDirectionImagePath = calcWinDirImagePath(settings.altWindDirectionImageLoc as String)
	setDateTimeFormats((String)settings.datetimeFormat)
	String distanceFormat = (settings.distanceFormat ?: 'Miles (mph)')
	String pressureFormat = (settings.pressureFormat ?: 'Inches')
	String rainFormat = (settings.rainFormat ?: 'Inches')
	String tempFormat = (settings.tempFormat ?: 'Fahrenheit (°F)')
	setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)
	String TWDDecimals = (settings.TWDDecimals ?: sZERO)
	String PDecimals = (settings.PDecimals ?: sZERO)
	String RDecimals = (settings.RDecimals ?: sZERO)
	setDisplayDecimals(TWDDecimals, PDecimals, RDecimals)

	Integer extSource = (settings.extSource.toInteger() ?: 2).toInteger()
	String pollIntervalStation = (settings.pollIntervalStation ?: '3 Hours')
	String pollLocationStation = (settings.pollLocationStation ?: 'http://')
	String pollIntervalForecast = (settings.pollIntervalForecast ?: '3 Hours')
	String pollIntervalForecastnight = (settings.pollIntervalForecastnight ?: '3 Hours')
	Boolean sourcefeelsLike = (settings.sourcefeelsLike ?: false)
	Boolean sourceIllumination = (settings.sourceIllumination ?: false)
	Boolean sourceUV = (settings.sourceUV ?: false)
	Boolean sourceWind = (settings.sourceWind ?: false)
	pollOWMl()
	if(settings.alertSource==sTWO) {pollWDG()}
}
void pollOWMl() {
/*  for testing a different Lat/Lon location uncommnent the two lines below */
//	String altLat = "38.627003" //"44.809122" // "40.6" //"30.6953657"
//	String altLon = "-90.199402" //"-68.735892" // "-75.43" //-88.0398912"
	Map ParamsOWMl = [ uri: 'https://api.openweathermap.org/data/2.5/find?lat=' + (String)altLat + '&lon=' + (String)altLon + '&cnt=1&appid=' + (String)apiKey, timeout: 20 ]
	logInfo('Poll OpenWeatherMap.org Location: ' + redactApiKey(ParamsOWMl.toString()))
	asynchttpGet('pollOWMlHandler', ParamsOWMl)
}
void pollOWMlHandler(resp, data) {
	logInfo('Polling OpenWeatherMap.org Location')
	if(resp.getStatus() != 200 && resp.getStatus() != 207) {
		logWarn('Calling ' + redactApiKey('https://api.openweathermap.org/data/2.5/find?lat=' + (String)altLat + '&lon=' + (String)altLon + '&cnt=1&appid=' + (String)apiKey))
		logWarn(resp.getStatus() + sCOLON + resp.getErrorMessage())
		state.OWML = sSPC
	}else{
		Map owml
		try {
			owml = parseJson(resp.data)
		} catch (Exception e) {
			logError('pollOWMlHandler: failed to parse JSON response from OpenWeatherMap, skipping this poll: ' + e.message)
			return
		}
		if(owml.toString()==sNULL) {
			pauseExecution(1000)
			pollOWMl()
			return
		}
		logInfo('OpenWeatherMap Location Data: ' + owml.toString())
		state.OWML = (owml?.list[0]?.id==null ? sSPC : owml?.list[0]?.id.toString())
		logInfo('OWM Location City Code: ' + state.OWML)
	}
}


void initialize_poll() {
	unschedule('pollWD')
	unschedule('pollOWM')
	Random rand = new Random(wnow())
	Integer ssseconds = rand.nextInt(60)
	Integer minutes2 = rand.nextInt(2)
	Integer minutes5 = rand.nextInt(5)
	Integer minutes10 = rand.nextInt(10)
	Integer minutes15 = rand.nextInt(15)
	Integer minutes30 = rand.nextInt(30)
	Integer minutes60 = rand.nextInt(60)
	Integer hours3 = rand.nextInt(3)
	Integer dsseconds
	Integer wdseconds
	if(ssseconds < 52 ){
		wdseconds = ssseconds + 4
		dsseconds = wdseconds + 4
	}else if(ssseconds < 56 ){
		wdseconds = ssseconds + 4
		dsseconds = wdseconds - 60 + 4
	}else{
		wdseconds = ssseconds - 60 + 4
		dsseconds = wdseconds + 4
	}   
	String pollIntervalFcst = (settings.pollIntervalForecast ?: '3 Hours')
	String pollIntervalFcstnight = (settings.pollIntervalForecastnight ?: '3 Hours')
	if(state.is_light==sTRU) {
		myFcstPoll = pollIntervalFcst
	}else{
		myFcstPoll = pollIntervalFcstnight
	}
	if(myFcstPoll == 'Manual Poll Only'){
		logInfo('MANUAL FORECAST POLLING ONLY')
	}else{
		myFcstPoll = myFcstPoll.replace(sSPC,sBLK)
		String myFcstSched
		logInfo('pollInterval: ' + myFcstPoll)
		switch(myFcstPoll) {
			case '2Minutes':
				myFcstSched = "${dsseconds} ${minutes2}/2 * * * ? *"
				break
			case '5Minutes':
				myFcstSched = "${dsseconds} ${minutes5}/5 * * * ? *"
				break
			case '10Minutes':
				myFcstSched = "${dsseconds} ${minutes10}/10 * * * ? *"
				break
			case '15Minutes':
				myFcstSched = "${dsseconds} ${minutes15}/15 * * * ? *"
				break
			case '30Minutes':
				myFcstSched = "${dsseconds} ${minutes30}/30 * * * ? *"
				break
			case '1Hour':
				myFcstSched = "${dsseconds} ${minutes60} * * * ? *"
				break
			case '3Hours':
			defa:
				myFcstSched = "${dsseconds} ${minutes60} ${hours3}/3 * * ? *"
		}
		schedule(myFcstSched, 'pollOWM')
	}
	String pollIntervalStation = (settings.pollIntervalStation ?: '3 Hours')
	String myStationPoll = pollIntervalStation
	if(myStationPoll == 'Manual Poll Only'){
		logInfo('MANUAL STATION POLLING ONLY')
	}else{
		myStationPoll = myStationPoll.replace(sSPC,sBLK)
		String myStationSched
		logInfo('myStationPoll: ' + myStationPoll)
		switch(myStationPoll) {
			case '1Minute':
				myStationSched = "${dsseconds} * * * * ? *"
				break
			case '2Minutes':
				myStationSched = "${dsseconds} ${minutes2}/2 * * * ? *"
				break
			case '5Minutes':
				myStationSched = "${dsseconds} ${minutes5}/5 * * * ? *"
				break
			case '10 Minutes':
				myStationSched = "${dsseconds} ${minutes10}/10 * * * ? *"
				break
			case '15Minutes':
				myStationSched = "${dsseconds} ${minutes15}/15 * * * ? *"
				break
			case '30Minutes':
				myStationSched = "${dsseconds} ${minutes30}/30 * * * ? *"
				break
			case '1Hour':
				myStationSched = "${dsseconds} ${minutes60} * * * ? *"
				break
			case '3Hours':
			defa:
				myStationSched = "${dsseconds} ${minutes60} ${hours3}/3 * * ? *"
		}
		schedule(myStationSched, 'pollWD')
	}
}

void pollData() {
	pollWD()
	if (extSource.toInteger() == 2) { pollOWM() }
	return
}
// ************************************************************************************************

void setDateTimeFormats(String formatselector){
	String mSel = formatselector ?: sONE
	String DTFormat
	String dateFormat
	String timeFormat
	switch(mSel) {
		case sONE: DTFormat = 'M/d/yyyy h:mm a';   dateFormat = 'M/d/yyyy';   timeFormat = 'h:mm a'; break
		case sTWO: DTFormat = 'M/d/yyyy HH:mm';	dateFormat = 'M/d/yyyy';   timeFormat = 'HH:mm';  break
		case '3': DTFormat = 'MM/dd/yyyy h:mm a'; dateFormat = 'MM/dd/yyyy'; timeFormat = 'h:mm a'; break
		case '4': DTFormat = 'MM/dd/yyyy HH:mm';  dateFormat = 'MM/dd/yyyy'; timeFormat = 'HH:mm';  break
		case '5': DTFormat = 'd/M/yyyy h:mm a';   dateFormat = 'd/M/yyyy';   timeFormat = 'h:mm a'; break
		case '6': DTFormat = 'd/M/yyyy HH:mm';	dateFormat = 'd/M/yyyy';   timeFormat = 'HH:mm';  break
		case '7': DTFormat = 'dd/MM/yyyy h:mm a'; dateFormat = 'dd/MM/yyyy'; timeFormat = 'h:mm a'; break
		case '8': DTFormat = 'dd/MM/yyyy HH:mm';  dateFormat = 'dd/MM/yyyy'; timeFormat = 'HH:mm';  break
		case '9': DTFormat = 'yyyy/MM/dd HH:mm';  dateFormat = 'yyyy/MM/dd'; timeFormat = 'HH:mm';  break
		defa: DTFormat = 'M/d/yyyy h:mm a';  dateFormat = 'M/d/yyyy';   timeFormat = 'h:mm a'; break
	}
	state.DTFormat = DTFormat
	state.dateFormat = dateFormat
	state.timeFormat = timeFormat
}

void setMeasurementMetrics(String distFormat, String pressFormat, String precipFormat, String temptFormat){
	String dMetric
	String pMetric
	String rMetric
	String tMetric
	if(distFormat == 'Miles (mph)') {
		dMetric = 'MPH'
	} else if(distFormat == 'knots') {
		dMetric = 'knots'
	} else if(distFormat == 'Kilometers (kph)') {
		dMetric = 'KPH'
	}else{
		dMetric = 'm/s'
	}
	state[sDMETR] = dMetric

	if(pressFormat == 'Millibar') {
		pMetric = 'MBAR'
	} else if(pressFormat == 'Inches') {
		pMetric = 'inHg'
	}else{
		pMetric = 'hPa'
	}
	state[sPMETR] = pMetric

	if(precipFormat == 'Millimeters') {
		rMetric = 'mm'
	}else{
		rMetric = 'in'
	}
	state[sRMETR] = rMetric

	if(temptFormat == 'Fahrenheit (°F)') {
		tMetric = sDF
	}else{
		tMetric = '°C'
	}
	state[sTMETR] = tMetric
}

void setDisplayDecimals(String TWDDisp, String PressDisp, String RainDisp) {
	String ddisp_twd
	String mult_twd
	String ddisp_p
	String mult_p
	String ddisp_r
	String mult_r
	switch(TWDDisp) {
		case sZERO: ddisp_twd = '%3.0f'; mult_twd = sONE; break
		case sONE: ddisp_twd = '%3.1f'; mult_twd = '10'; break
		case sTWO: ddisp_twd = '%3.2f'; mult_twd = '100'; break
		case '3': ddisp_twd = '%3.3f'; mult_twd = '1000'; break
		case '4': ddisp_twd = '%3.4f'; mult_twd = '10000'; break
		defa: ddisp_twd = '%3.0f'; mult_twd = sONE; break
	}
	state.ddisp_twd = ddisp_twd
	state.mult_twd = mult_twd
	switch(PressDisp) {
		case sZERO: ddisp_p = '%,4.0f'; mult_p = sONE; break
		case sONE: ddisp_p = '%,4.1f'; mult_p = '10'; break
		case sTWO: ddisp_p = '%,4.2f'; mult_p = '100'; break
		case '3': ddisp_p = '%,4.3f'; mult_p = '1000'; break
		case '4': ddisp_p = '%,4.4f'; mult_p = '10000'; break
		defa: ddisp_p = '%,4.0f'; mult_p = sONE; break
	}
	state.ddisp_p = ddisp_p
	state.mult_p = mult_p
	switch(RainDisp) {
		case sZERO: ddisp_r = '%2.0f'; mult_r = sONE; break
		case sONE: ddisp_r = '%2.1f'; mult_r = '10'; break
		case sTWO: ddisp_r = '%2.2f'; mult_r = '100'; break
		case '3': ddisp_r = '%2.3f'; mult_r = '1000'; break
		case '4': ddisp_r = '%2.4f'; mult_r = '10000'; break
		defa: ddisp_r = '%2.0f'; mult_r = sONE; break
	}
	state.ddisp_r = ddisp_r
	state.mult_r = mult_r
}

def estimateLux(Integer condition_id, Integer cloud)	 {	
	Long lux
	Boolean aFCC = true
	Double l
	String bwn
	TimeZone tZ					= TimeZone.getDefault() //TimeZone.getTimeZone(tz_id)
	String lT					= new Date().format('yyyy-MM-dd\'T\'HH:mm:ssXXX', tZ)
	Long localeMillis			= getEpoch(lT)
	Long twilight_beginMillis
	Long sunriseTimeMillis
	Long noonTimeMillis
	Long sunsetTimeMillis
	Long twilight_endMillis

	Date dnow= new Date()
	String currDate = dnow.format('yyyy-MM-dd', tZ)
	Date tSunrise, tSunset
	tSunrise = (Date)todaysSunrise
	tSunrise = (!tSunrise || tSunrise == null) ? Date.parse("yyyy-MM-dd hh:mm:ss", currDate + " 00:00:00") : tSunrise

	tSunset = (Date)todaysSunset
	if(!tSunset || tSunset == null){
		String currYear = dnow.format('yyyy', tZ)
		Date mar21= Date.parse("yyyy-MM-dd", currYear + '-03-21')
		Date sep21= Date.parse("yyyy-MM-dd", currYear + '-09-21')
		Boolean isBtwn= (dnow >= mar21 && dnow < sep21)
		Date twelve59= Date.parse("yyyy-MM-dd hh:mm:ss", currDate + " 23:59:59")
		Date mid01= Date.parse("yyyy-MM-dd hh:mm:ss", currDate + " 00:00:01")
		if(altLat.toDouble() > 0.0D) {
			tSunset = isBtwn ? twelve59 : mid01
		} else {
			tSunset = !isBtwn ? twelve59 : mid01
		}
	}
        
    twilight_beginMillis	= tSunrise.getTime() - 1500000L // (25*60*1000) 25 minutes before sunrise
    sunriseTimeMillis	= tSunrise.getTime()
    noonTimeMillis		= tSunrise.getTime() + (tSunset.getTime() - tSunrise.getTime()).intdiv(2)
    sunsetTimeMillis	= tSunset.getTime()
    twilight_endMillis	= tSunset.getTime() + 1500000L // (25*60*1000) 25 minutes after sunset
	Long twiStartNextMillis   = twilight_beginMillis + 86400000L // = 24*60*60*1000 --> one day in milliseconds
	Long sunriseNextMillis	= sunriseTimeMillis + 86400000L
	Long noonTimeNextMillis   = noonTimeMillis + 86400000L
	Long sunsetNextMillis	 = sunsetTimeMillis + 86400000L
	Long twiEndNextMillis	 = twilight_endMillis + 86400000L

	switch(localeMillis) { 
		case { it < twilight_beginMillis}: 
			bwn = 'Fully Night Time'
			lux = 5l
			aFCC = false
			break
		case { it < sunriseTimeMillis}:
			bwn = 'between twilight and sunrise'
			l = (((localeMillis - twilight_beginMillis) * 50f) / (sunriseTimeMillis - twilight_beginMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as Long)
			break
		case { it < noonTimeMillis}:
			bwn = 'between sunrise and noon'
			l = (((localeMillis - sunriseTimeMillis) * 10000f) / (noonTimeMillis - sunriseTimeMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as Long)
			break
		case { it < sunsetTimeMillis}:
			bwn = 'between noon and sunset'
			l = (((sunsetTimeMillis - localeMillis) * 10000f) / (sunsetTimeMillis - noonTimeMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as Long)
			break
		case { it < twilight_endMillis}:
			bwn = 'between sunset and twilight'
			l = (((twilight_endMillis - localeMillis) * 50f) / (twilight_endMillis - sunsetTimeMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as Long)
			break
		case { it < twiStartNextMillis}:
			bwn = 'Fully Night Time'
			lux = 5l
			aFCC = false
			break
		case { it < sunriseNextMillis}:
			bwn = 'between twilight and sunrise'
			l = (((localeMillis - twiStartNextMillis) * 50f) / (sunriseNextMillis - twiStartNextMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as Long)
			break
		case { it < noonTimeNextMillis}:
			bwn = 'between sunrise and noon'
			l = (((localeMillis - sunriseNextMillis) * 10000f) / (noonTimeNextMillis - sunriseNextMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as Long)
			break
		case { it < sunsetNextMillis}:
			bwn = 'between noon and sunset'
			l = (((sunsetNextMillis - localeMillis) * 10000f) / (sunsetNextMillis - noonTimeNextMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as Long)
			break
		case { it < twiEndNextMillis}:
			bwn = 'between sunset and twilight'
			l = (((twiEndNextMillis - localeMillis) * 50f) / (twiEndNextMillis - sunsetNextMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as Long)
			break
		defa:
			bwn = 'Fully Night Time'
			lux = 5l
			aFCC = false
			break
	}
	String cC = condition_id.toString()
	String cCT; cCT = ' using cloud cover from API'
	Double cCF; cCF = (!cloud || cloud==sBLK) ? 0.998d : (1 - (cloud/100 / 3d))
    if(aFCC){
		if(!cloud){
			Map LUitem = LUTable.find{ Map it -> (Integer)it.id == condition_id }
			if (LUitem)	{
				cCF = LUitem.luxp
				cCT = ' using estimated cloud cover based on condition.'
			}else{
				cCF = 1.0
				cCT = ' cloud coverage not available now.'
			}
		}
	}
	lux = (lux * cCF) as Long
	Boolean t_jitter = (!settings.luxjitter) ? false : settings.luxjitter
	if(t_jitter){
		// reduce event variability  code from @nh.schottfam
		if(lux > 1100) {
			Long t0 = Math.round(lux/800)
			lux = t0 * 800
		} else if(lux <= 1100 && lux > 400) {
			Long t0 = Math.round(lux/400)
			lux = t0 * 400
		}else{
			lux = 5
		}
	}
	lux = Math.max(lux, 5)
	logInfo('estimateLux results: condition: ' + cC + ' | condition factor: ' + cCF + ' | condition text: ' + cCT + '| lux: ' + lux)
	return [lux, bwn]
}

private static Long getEpoch (String aTime) {
	TimeZone tZ = TimeZone.getDefault()
	Date localeTime = new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', aTime, tZ)
	Long localeMillis = localeTime.getTime()
	return (localeMillis)
}

void SummaryMessage(Boolean SType, String Slast_poll_date, String Slast_poll_time, String SforecastTemp, String Sprecip, String Svis){
	BigDecimal windgust
	if(state.wind_gust == sBLK || myGetDataBD('wind_gust') < 1.0 || state.wind_gust==sNULL) {
		windgust = 0.00g
	}else{
		windgust = myGetDataBD('wind_gust')
	}
	String wSum // = (String)null
	if(SType){
		wSum = 'Weather summary for ' + state.city + ' updated at ' + Slast_poll_time + ' on ' + Slast_poll_date + '. '
		wSum+= state.condition_text
		wSum+= (!SforecastTemp || SforecastTemp==sBLK) ? '. ' : SforecastTemp
		wSum+= 'Humidity is ' + state.humidity + '% and the temperature is ' + String.format(state.ddisp_twd as String, myGetDataBD(sTEMP)) + state[sTMETR] + '. '
		wSum+= 'The temperature feels like it is ' + String.format(state.ddisp_twd as String, myGetDataBD('feelsLike')) + state[sTMETR] + '. '
		wSum+= 'Wind: ' + state.wind_string + ', gusts: ' + ((windgust < 1.00) ? 'calm. ' : 'up to ' + windgust.toString() + sSPC + state[sDMETR] + '. ')
		wSum+= Sprecip
		wSum+= Svis
		wSum+= alertPublish ? ((!state.alert || state.alert==sNULL) ? sBLK : sSPC + state.alert + sDOT) : sBLK
	}else{
		wSum = state.condition_text + sSPC
		wSum+= ((!SforecastTemp || SforecastTemp==sBLK) ? '. ' : SforecastTemp)
		wSum+= ' Humidity: ' + state.humidity + '%. Temperature: ' + String.format(state.ddisp_twd as String, myGetDataBD(sTEMP)) + state[sTMETR] + '. '
		wSum+= state.wind_string + ', gusts: ' + ((windgust == 0.00) ? 'calm. ' : 'up to ' + windgust + state[sDMETR] + sDOT)
	}
	wSum = wSum.take(1024)
	sendIfChanged(name: 'weatherSummary', value: wSum)
}

String getImgName(Integer wCode, String iconTOD){
	Map LUitem = LUTable.find{ (Integer)it.id == wCode }
	logDebug('getImgName Inputs: ' + wCode.toString() + ', ' + iconTOD + ';  Result: ' + (iconTOD==sTRU ? (LUitem ? (String)LUitem.Icd : sNPNG) : (LUitem ? (String)LUitem.Icn : sNPNG)))
	return (iconTOD==sTRU ? (LUitem ? (String)LUitem.Icd : sNPNG) : (LUitem ? (String)LUitem.Icn : sNPNG))
}

String getCondCode(Integer cid, String iconTOD){
	Map LUitem = LUTable.find{ (Integer)it.id == cid }
	logDebug('getCondCode Inputs: ' + cid.toString() + ', ' + iconTOD + ';  Result: ' + (iconTOD==sTRU ? (LUitem ? (String)LUitem.sId : sNPNG) : (LUitem ? (String)LUitem.sIn : sNPNG)))
	return (iconTOD==sTRU ? (LUitem ? (String)LUitem.sId : sNPNG) : (LUitem ? (String)LUitem.sIn : sNPNG))
}

void settingsOff(){
	log.info 'Weather-Display Driver - INFO: Settings disabled...'
	device.updateSetting('settingEnable',[value:sFLS,type:'bool'])
}

// Redacts the OpenWeatherMap API key from a URL/log string unless aPIKeyExposedEnable is on.
String redactApiKey(String text) {
	if (settings.aPIKeyExposedEnable == true || text == null) return text
	return text.replaceAll(/appid=[^&\s]+/, 'appid=***')
}

private void logMessage(String level, String msg) {
	if (settings["log${level.capitalize()}Enable"] == true) {
		log."${level}"("Weather-Display Driver${level=='warn'?' WARNING':level=='error'?' ERROR':''}: ${msg}")
	}
}
void logDebug(String msg) { logMessage('debug', msg) }
void logInfo(String msg)  { logMessage('info', msg) }
void logTrace(String msg) { logMessage('trace', msg) }
void logWarn(String msg)  { logMessage('warn', msg) }
void logError(String msg) { logMessage('error', msg) }

/**
 * send event only if the value actually changed
 */
void sendIfChanged(Map args) {
	if (!args || !args.name) return
	String oldVal = device.currentValue(args.name as String)?.toString()
	String newVal = args.value != null ? args.value.toString() : ''
	if (oldVal != newVal) {
		sendEvent(name: args.name, value: args.value, descriptionText: args.descriptionText, unit: args.unit, displayed: args.displayed)
		logDebug('Event triggered: ' + args.name + ' -> ' + args.value)
	}
}

/**
 * send event (diffed) if enabled for publishing, otherwise delete current state
 */
void sendIfChangedPublish(Map args)	{
//	Purpose: Attribute sent to DB if selected
	if (settings."${args.name + 'Publish'}") {
		sendIfChanged(args)
    }else{
		device.deleteCurrentState((String)args.name)
	}
}

///
/// Calculations, ported/adapted from the community OpenWeatherMap Multi-API Weather Driver: sun/moon
/// altitude & azimuth (Meeus-style — also the single source now for the legacy altitude/azimuth
/// attributes, replacing a second, independent SunCalc-based implementation that used to compute
/// those same two attributes separately), moon phase detail, and consolidated wind direction.
///

private String calcMoonPhaseImagePath(String altMPLoc) {
	String base = altMPLoc ? altMPLoc.trim() : 'https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhaseRes/'
	if (!base.endsWith('/')) base += '/'
	return base
}

private String calcWinDirImagePath(String altWDLoc) {
	String base = (altWDLoc && altWDLoc.trim() != '') ? altWDLoc.trim() : ''
	if (base != '' && !base.endsWith('/')) base += '/'
	return base
}

Map calcWindDirection(degrees) {
	if (degrees == null) {
		return [cardinal: 'N', full: 'North', iconUrl: '', icon: '💨', emoji: '💨']
	}
	double deg = 0.0
	try {
		deg = (degrees as BigDecimal).doubleValue()
	} catch (Exception e) {
		logError('calcWindDirection failed to parse degrees (' + degrees + '): ' + e.message)
		return [cardinal: 'N', full: 'North', iconUrl: '', icon: '💨', emoji: '💨']
	}
	deg = (deg % 360 + 360) % 360

	List<String> cardinals = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW']
	List<String> fullWords = ['North', 'North-Northeast', 'Northeast', 'East-Northeast', 'East', 'East-Southeast', 'Southeast', 'South-Southeast', 'South', 'South-Southwest', 'Southwest', 'West-Southwest', 'West', 'West-Northwest', 'Northwest', 'North-Northwest']
	List<String> directionEmojis = ['⬇️', '↙️', '↙️', '↙️', '⬅️', '↖️', '↖️', '↖️', '⬆️', '↗️', '↗️', '↗️', '➡️', '↘️', '↘️', '↘️']

	int index = (int) Math.round(deg / 22.5) % 16
	String token = cardinals[index]
	String word = fullWords[index]
	String chosenEmoji = directionEmojis[index]

	String finalIconUrl = ''
	String finalIconDisplay = chosenEmoji
	String basePath = state.windDirectionImagePath ?: (calcWinDirImagePath(settings.altWindDirectionImageLoc as String) ?: '')
	if (basePath != null && basePath.trim() != '') {
		finalIconUrl = "${basePath}wind-${token.toLowerCase()}.png"
		finalIconDisplay = "<img src='${finalIconUrl}' style='height:1em;vertical-align:middle;'>"
	}

	return [cardinal: token, full: word, iconUrl: finalIconUrl, icon: finalIconDisplay, emoji: chosenEmoji]
}

private BigDecimal calcSunPosition() {
	int precision = (settings.precisionSunMoonAngles ?: '0').toInteger()
	BigDecimal locLat = (altLat as String)?.toBigDecimal()
	BigDecimal locLon = (altLon as String)?.toBigDecimal()
	if (locLat == null || locLon == null) {
		logWarn('calcSunPosition: Latitude or Longitude coordinates are unavailable.')
		state.altitude = 0.0
		state.azimuth = 0.0
		return 0.0
	}

	Calendar cal = Calendar.getInstance(TimeZone.getTimeZone('UTC'))
	double hour = cal.get(Calendar.HOUR_OF_DAY) + (cal.get(Calendar.MINUTE) / 60.0) + (cal.get(Calendar.SECOND) / 3600.0)
	int day = cal.get(Calendar.DAY_OF_MONTH)
	int month = cal.get(Calendar.MONTH) + 1
	int year = cal.get(Calendar.YEAR)
	if (month <= 2) { year -= 1; month += 12 }
	int A = (int)(year / 100)
	int B = 2 - A + (int)(A / 4)
	double jd = (int)(365.25 * (year + 4716)) + (int)(30.6001 * (month + 1)) + day + (hour / 24.0) + B - 1524.5
	double d = jd - 2451545.0

	double g = 357.529 + 0.98560028 * d
	double q = 280.459 + 0.98564736 * d
	double L = q + 1.915 * Math.sin(Math.toRadians(g)) + 0.020 * Math.sin(Math.toRadians(2 * g))
	double e2 = 23.439 - 0.00000036 * d

	double sin_delta = Math.sin(Math.toRadians(e2)) * Math.sin(Math.toRadians(L))
	double delta = Math.toDegrees(Math.asin(sin_delta))
	double ra = Math.toDegrees(Math.atan2(Math.cos(Math.toRadians(e2)) * Math.sin(Math.toRadians(L)), Math.cos(Math.toRadians(L))))

	double gst = 280.46061837 + 360.98564736629 * d
	double lst = gst + locLon.doubleValue()
	double H = lst - ra

	double latRad = Math.toRadians(locLat.doubleValue())
	double deltaRad = Math.toRadians(delta)
	double hRad = Math.toRadians(H)

	double sin_alt = Math.sin(latRad) * Math.sin(deltaRad) + Math.cos(latRad) * Math.cos(deltaRad) * Math.cos(hRad)
	sin_alt = Math.max(-1.0, Math.min(1.0, sin_alt))
	double sAlt = Math.toDegrees(Math.asin(sin_alt))

	double cos_alt = Math.cos(Math.toRadians(sAlt))
	double az = 0.0
	if (Math.abs(cos_alt) > 0.0001) {
		double cos_az = (Math.sin(deltaRad) - Math.sin(latRad) * sin_alt) / (Math.cos(latRad) * cos_alt)
		cos_az = Math.max(-1.0, Math.min(1.0, cos_az))
		az = Math.toDegrees(Math.acos(cos_az))
		if (Math.sin(hRad) > 0) az = 360.0 - az
	} else {
		az = (locLat > 0) ? 180.0 : 0.0
	}
	az = (az % 360.0 + 360.0) % 360.0

	BigDecimal finalSunAltitude = BigDecimal.valueOf(sAlt).setScale(precision, java.math.RoundingMode.HALF_UP)
	BigDecimal finalSunAzimuth = BigDecimal.valueOf(az).setScale(precision, java.math.RoundingMode.HALF_UP)

	state.currentSunAltitude = finalSunAltitude
	state.currentSunAzimuth = finalSunAzimuth
	// Raw (unrounded) values, kept separately for the legacy 'altitude'/'azimuth' attributes, which have
	// always reported full double precision rather than the user-configurable precisionSunMoonAngles rounding.
	state.altitude = sAlt
	state.azimuth = az
	return finalSunAltitude
}

private BigDecimal calcMoonPosition() {
	int precision = (settings.precisionSunMoonAngles ?: '0').toInteger()
	BigDecimal locLat = (altLat as String)?.toBigDecimal()
	BigDecimal locLon = (altLon as String)?.toBigDecimal()
	if (locLat == null || locLon == null) {
		logWarn('calcMoonPosition: Latitude or Longitude coordinates are unavailable.')
		return 0.0
	}

	Calendar cal = Calendar.getInstance(TimeZone.getTimeZone('UTC'))
	double hour = cal.get(Calendar.HOUR_OF_DAY) + (cal.get(Calendar.MINUTE) / 60.0) + (cal.get(Calendar.SECOND) / 3600.0)
	int day = cal.get(Calendar.DAY_OF_MONTH)
	int month = cal.get(Calendar.MONTH) + 1
	int year = cal.get(Calendar.YEAR)
	if (month <= 2) { year -= 1; month += 12 }
	int A = (int)(year / 100)
	int B = 2 - A + (int)(A / 4)
	double jd = (int)(365.25 * (year + 4716)) + (int)(30.6001 * (month + 1)) + day + (hour / 24.0) + B - 1524.5
	double d = jd - 2451545.0

	double L = 218.316 + 13.176396 * d
	double M = 134.963 + 13.064993 * d
	double F = 93.272 + 13.229350 * d

	double lRad = Math.toRadians(L + 6.289 * Math.sin(Math.toRadians(M)))
	double bRad = Math.toRadians(5.128 * Math.sin(Math.toRadians(F)))
	double e2 = Math.toRadians(23.439 - 0.00000036 * d)

	double sin_dec = Math.sin(bRad) * Math.cos(e2) + Math.cos(bRad) * Math.sin(e2) * Math.sin(lRad)
	sin_dec = Math.max(-1.0, Math.min(1.0, sin_dec))
	double decRad = Math.asin(sin_dec)

	double y = Math.sin(lRad) * Math.cos(e2) - Math.tan(bRad) * Math.sin(e2)
	double x = Math.cos(lRad)
	double raDeg = Math.toDegrees(Math.atan2(y, x))

	double gst = 280.46061837 + 360.98564736629 * d
	double lst = gst + locLon.doubleValue()
	double H = lst - raDeg

	double latRad = Math.toRadians(locLat.doubleValue())
	double hRad = Math.toRadians(H)

	double sin_alt = Math.sin(latRad) * Math.sin(decRad) + Math.cos(latRad) * Math.cos(decRad) * Math.cos(hRad)
	sin_alt = Math.max(-1.0, Math.min(1.0, sin_alt))
	double mAlt = Math.toDegrees(Math.asin(sin_alt))

	double cos_alt = Math.cos(Math.toRadians(mAlt))
	double az = 0.0
	if (Math.abs(cos_alt) > 0.0001) {
		double cos_az = (Math.sin(decRad) - Math.sin(latRad) * sin_alt) / (Math.cos(latRad) * cos_alt)
		cos_az = Math.max(-1.0, Math.min(1.0, cos_az))
		az = Math.toDegrees(Math.acos(cos_az))
		if (Math.sin(hRad) > 0) az = 360.0 - az
	} else {
		az = (locLat > 0) ? 180.0 : 0.0
	}
	az = (az % 360.0 + 360.0) % 360.0

	BigDecimal finalMoonAltitude = BigDecimal.valueOf(mAlt).setScale(precision, java.math.RoundingMode.HALF_UP)
	BigDecimal finalMoonAzimuth = BigDecimal.valueOf(az).setScale(precision, java.math.RoundingMode.HALF_UP)

	state.currentMoonAltitude = finalMoonAltitude
	state.currentMoonAzimuth = finalMoonAzimuth
	return finalMoonAltitude
}

Map calcMoonPhaseValue(Map todayData = [:], Map tomData = [:], Map tdaData = [:]) {
	List<String> emojis = ['🌑', '🌒', '🌓', '🌔', '🌕', '🌖', '🌗', '🌘']
	def phases = [
		[sourceMap: todayData, valAttr: 'todayMoonPhase', pngAttr: 'todayMoonPhasePngImageUrl', textAttr: 'todayMoonPhaseText', emojiAttr: 'todayMoonPhaseEmojiIcon'],
		[sourceMap: tomData,   valAttr: 'tomMoonPhase',   pngAttr: 'tomMoonPhasePngImageUrl',   textAttr: 'tomMoonPhaseText',   emojiAttr: 'tomMoonPhaseEmojiIcon'],
		[sourceMap: tdaData,   valAttr: 'tdaMoonPhase',   pngAttr: 'tdaMoonPhasePngImageUrl',   textAttr: 'tdaMoonPhaseText',   emojiAttr: 'tdaMoonPhaseEmojiIcon']
	]
	boolean isPathOverridden = (settings.altMoonPhaseImagePath != null && (settings.altMoonPhaseImagePath as String).trim() != '')
	Map resultMap = [:]

	phases.each { phase ->
		def rawVal = (phase.sourceMap && phase.sourceMap['moon_phase'] != null) ? phase.sourceMap['moon_phase'] : device.currentValue(phase.valAttr as String)
		if (rawVal != null && rawVal.toString().isNumber()) {
			double val = (rawVal.toDouble() % 1.0 + 1.0) % 1.0
			int index = (int) Math.floor((val * 8) + 0.5) % 8
			String filename = isPathOverridden ? "mp${index}.png" : "moon-phase-icon-${index}.png"
			String basePath = state.moonPhaseImagePath ?: 'https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhaseRes/'
			String fullPath = "${basePath}${filename}"
			sendIfChanged(name: phase.pngAttr as String, value: fullPath)

			String chosenEmoji = emojis[index]
			sendIfChanged(name: phase.emojiAttr as String, value: chosenEmoji)

			String phaseName = 'Unknown'
			double tolerance = 0.02
			if (val <= tolerance || val >= (1.0 - tolerance)) phaseName = 'New Moon'
			else if (val > tolerance && val < (0.25 - tolerance)) phaseName = 'Waxing Crescent'
			else if (val >= (0.25 - tolerance) && val <= (0.25 + tolerance)) phaseName = 'First Quarter'
			else if (val > (0.25 + tolerance) && val < (0.5 - tolerance)) phaseName = 'Waxing Gibbous'
			else if (val >= (0.5 - tolerance) && val <= (0.5 + tolerance)) phaseName = 'Full Moon'
			else if (val > (0.5 + tolerance) && val < (0.75 - tolerance)) phaseName = 'Waning Gibbous'
			else if (val >= (0.75 - tolerance) && val <= (0.75 + tolerance)) phaseName = 'Last Quarter'
			else if (val > (0.75 + tolerance) && val < (1.0 - tolerance)) phaseName = 'Waning Crescent'
			sendIfChanged(name: phase.textAttr as String, value: phaseName)
			sendIfChanged(name: phase.valAttr as String, value: val)

			String svgContent = calcMoonPhaseSvgMask(val, state.moonPhaseImagePath as String ?: 'https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhaseRes/')
			sendIfChanged(name: (phase.valAttr as String) + 'SvgImage', value: svgContent)

			if (phase.valAttr == 'todayMoonPhase') {
				resultMap.text = phaseName
				resultMap.png = fullPath
				resultMap.emoji = chosenEmoji
			}
		}
	}
	return resultMap
}

String calcMoonPhaseSvgMask(double phase, String path) {
	if (phase <= 0.02 || phase >= 0.98) {
		return """<svg viewBox="0 0 256 256" style="width:100%;height:100%;display:block;"><circle cx="128" cy="128" r="127" fill="#121214"/></svg>"""
	}
	double rx1 = 127.0, rx2 = 127.0
	int sf1 = 1, sf2 = 1
	if (phase <= 0.25) { rx1 *= (1 - 4 * phase) }
	else if (phase <= 0.50) { rx1 *= (4 * phase - 1); sf1 = 0 }
	else if (phase <= 0.75) { rx2 *= (3 - 4 * phase); sf2 = 0 }
	else { rx2 *= (4 * phase - 3) }
	return """<svg viewBox="0 0 256 256" style="width:100%;height:100%;display:block;"><filter id="b"><feGaussianBlur stdDeviation="6"/></filter><mask id="a"><path d="M128,1A${rx1.round(1)},127 180 0 $sf1 128,255A${rx2.round(1)},127 180 0 $sf2 128,1z" fill="#fff" filter="url(#b)"/></mask><radialGradient id="s"><stop offset="10%" stop-color="#0007"/><stop offset="90%" stop-color="#000d"/></radialGradient><image width="256" height="256" href="${path}lunar_surface.png"/><circle cx="128" cy="128" r="127" mask="url(#a)" fill="url(#s)"/></svg>"""
}

void clearAllDriverStates() {
	logInfo('Clearing all driver states...')
	state.clear()
	logInfo('All states have been cleared.')
}

void clearAllAttributes() {
	logInfo('Clearing all attributes...')
	device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
	logInfo('All attributes have been cleared.')
}

void clearAllSchedules() {
	logInfo('Clearing all scheduled jobs (including orphaned schedules)...')
	unschedule()
	logInfo('All scheduled jobs have been successfully cleared.')
}

@Field final List<Map>	LUTable =	 [
[id: 200, Icd: '38.png', Icn: '47.png', luxp: 0.2, sId: sCTS, sIn: sNCTS],
[id: 201, Icd: '38.png', Icn: '47.png', luxp: 0.2, sId: sCTS, sIn: sNCTS],
[id: 202, Icd: '38.png', Icn: '47.png', luxp: 0.2, sId: sCTS, sIn: sNCTS],
[id: 210, Icd: '38.png', Icn: '47.png', luxp: 0.2, sId: sCTS, sIn: sNCTS],
[id: 211, Icd: '38.png', Icn: '47.png', luxp: 0.2, sId: sCTS, sIn: sNCTS],
[id: 212, Icd: '38.png', Icn: '47.png', luxp: 0.2, sId: sCTS, sIn: sNCTS],
[id: 221, Icd: '38.png', Icn: '47.png', luxp: 0.2, sId: sCTS, sIn: sNCTS],
[id: 230, Icd: '38.png', Icn: '47.png', luxp: 0.2, sId: sCTS, sIn: sNCTS],
[id: 231, Icd: '38.png', Icn: '47.png', luxp: 0.2, sId: sCTS, sIn: sNCTS],
[id: 232, Icd: '38.png', Icn: '47.png', luxp: 0.2, sId: sCTS, sIn: sNCTS],
[id: 300, Icd: s9, Icn: s9, luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 301, Icd: s9, Icn: s9, luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 302, Icd: s9, Icn: s9, luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 310, Icd: s9, Icn: s9, luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 311, Icd: s9, Icn: s9, luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 312, Icd: s9, Icn: s9, luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 313, Icd: s9, Icn: s9, luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 314, Icd: s9, Icn: s9, luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 321, Icd: s9, Icn: s9, luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 500, Icd: s39, Icn: s9, luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 501, Icd: s39, Icn: '11.png', luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 502, Icd: s39, Icn: '11.png', luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 503, Icd: s39, Icn: '11.png', luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 504, Icd: s39, Icn: '11.png', luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 511, Icd: s39, Icn: '11.png', luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 520, Icd: s39, Icn: s9, luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 521, Icd: s39, Icn: '11.png', luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 522, Icd: s39, Icn: '11.png', luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 531, Icd: s39, Icn: s9, luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 600, Icd: '13.png', Icn: '46.png', luxp: 0.4, sId: 'flurries', sIn: 'nt_snow'],
[id: 601, Icd: '14.png', Icn: '46.png', luxp: 0.3, sId: 'snow', sIn: 'nt_snow'],
[id: 602, Icd: '16.png', Icn: '46.png', luxp: 0.3, sId: 'snow', sIn: 'nt_snow'],
[id: 611, Icd: s9, Icn: '46.png', luxp: 0.5, sId: sRAIN, sIn: 'nt_snow'],
[id: 612, Icd: '8.png', Icn: '46.png', luxp: 0.5, sId: 'sleet', sIn: 'nt_snow'],
[id: 613, Icd: s9, Icn: '46.png', luxp: 0.5, sId: sRAIN, sIn: 'nt_snow'],
[id: 615, Icd: s39, Icn: '45.png', luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 616, Icd: s39, Icn: '45.png', luxp: 0.5, sId: sRAIN, sIn: sNRAIN],
[id: 620, Icd: '13.png', Icn: '46.png', luxp: 0.4, sId: 'flurries', sIn: 'nt_snow'],
[id: 621, Icd: '16.png', Icn: '46.png', luxp: 0.3, sId: 'snow', sIn: 'nt_snow'],
[id: 622, Icd: '42.png', Icn: '42.png', luxp: 0.6, sId: 'snow', sIn: 'nt_snow'],
[id: 701, Icd: s23, Icn: s23, luxp: 0.8, sId: sPCLDY, sIn: sNPCLDY],
[id: 711, Icd: s23, Icn: s23, luxp: 0.8, sId: sPCLDY, sIn: sNPCLDY],
[id: 721, Icd: s23, Icn: s23, luxp: 0.8, sId: sPCLDY, sIn: sNPCLDY],
[id: 731, Icd: s23, Icn: s23, luxp: 0.8, sId: sPCLDY, sIn: sNPCLDY],
[id: 741, Icd: s23, Icn: s23, luxp: 0.8, sId: sPCLDY, sIn: sNPCLDY],
[id: 751, Icd: s23, Icn: s23, luxp: 0.8, sId: sPCLDY, sIn: sNPCLDY],
[id: 761, Icd: s23, Icn: s23, luxp: 0.8, sId: sPCLDY, sIn: sNPCLDY],
[id: 762, Icd: s23, Icn: s23, luxp: 0.8, sId: sPCLDY, sIn: sNPCLDY],
[id: 771, Icd: s23, Icn: s23, luxp: 0.8, sId: sPCLDY, sIn: sNPCLDY],
[id: 781, Icd: s23, Icn: s23, luxp: 0.8, sId: sPCLDY, sIn: sNPCLDY],
[id: 800, Icd: '32.png', Icn: '31.png', luxp: 1, sId: 'clear', sIn: 'nt_clear'],
[id: 801, Icd: '34.png', Icn: '33.png', luxp: 0.9, sId: sPCLDY, sIn: sNPCLDY],
[id: 802, Icd: '30.png', Icn: '29.png', luxp: 0.8, sId: sPCLDY, sIn: sNPCLDY],
[id: 803, Icd: '28.png', Icn: '27.png', luxp: 0.6, sId: 'mostlycloudy', sIn: 'nt_mostlycloudy'],
[id: 804, Icd: '26.png', Icn: '26.png', luxp: 0.6, sId: 'cloudy', sIn: 'nt_cloudy'],
[id: 999, Icd: sNPNG, Icn: sNPNG, luxp: 1.0, sId: 'unknown', sIn: 'unknown'],
]

@Field final Map<String,Map> attributesMap = [
	'threedayTile':				[title: 'Three Day Forecast Tile', d: 'Display Three Day Forecast Tile?', ty: false, defa: sFLS],
	'alert':					[title: 'Weather Alert', d: 'Display any weather alert?', ty: false, defa: sFLS],
	'betwixt':					[title: 'Slice of Day', d: 'Display the \'slice-of-day\'?', ty: sSTR, defa: sFLS],
	'cloud':					[title: 'Cloud', d: 'Display cloud coverage %?', ty: sNUM, defa: sFLS],
	'cloudExtended':			[title: 'Cloud Forecast', d: 'Display cloud coverage forecast?', ty: false, defa: sFLS],
	'condition_code':			[title: 'Condition Code', d: 'Display \'condition_code\'?', ty: sSTR, defa: sFLS],
	'condition_icon_only':		[title: 'Condition Icon Only', d: 'Display \'condition_code_only\'?', ty: sSTR, defa: sFLS],
	'condition_icon_url':		[title: 'Condition Icon URL', d: 'Display \'condition_code_url\'?', ty: sSTR, defa: sFLS],
	'condition_icon':			[title: 'Condition Icon', d: 'Display \'condition_icon\'?', ty: sSTR, defa: sFLS],
	'condition_iconWithText':   [title: 'Condition Icon With Text', d: 'Display \'condition_iconWithText\'?', ty: sSTR, defa: sFLS],
	'condition_text':			[title: 'Condition Text', d: 'Display \'condition_text\'?', ty: sSTR, defa: sFLS],
	'country':					[title: 'Country', d: 'Display \'country\'?', ty: sSTR, defa: sFLS],
	'dashHubitatOWM':			[title: 'Dash - Hubitat and OpenWeatherMap', d: 'Display attributes required by Hubitat and OpenWeatherMap dashboards?', ty: false, defa: sFLS],
	'dashSmartTiles':			[title: 'Dash - SmartTiles', d: 'Display attributes required by SmartTiles dashboards?', ty: false, defa: sFLS],
	'dashSharpTools':			[title: 'Dash - SharpTools.io', d: 'Display attributes required by SharpTools.io?', ty: false, defa: sFLS],
	'dewpoint':					[title: 'Dewpoint (in default unit)', d: 'Display the dewpoint?', ty: sNUM, defa: sFLS],
	'fcstHighLow':				[title: 'Forecast High/Low Temperatures:', d: 'Display forecast High/Low temperatures?', ty: false, defa: sFLS],
	'forecast_code':			[title: 'Forecast Code', d: 'Display \'forecast_code\'?', ty: sSTR, defa: sFLS],
	'forecast_text':			[title: 'Forecast Text', d: 'Display \'forecast_text\'?', ty: sSTR, defa: sFLS],
	'illuminated':				[title: 'Illuminated', d: 'Display \'illuminated\' (with \'lux\' added for use on a Dashboard)?', ty: sSTR, defa: sFLS],
	'is_day':					[title: 'Is daytime', d: 'Display \'is_day\'?', ty: sNUM, defa: sFLS],
	'localSunrise':				[title: 'Local SunRise and SunSet', d: 'Display the Group of \'Time of Local Sunrise and Sunset\', with and without Dashboard text?', ty: false, defa: sFLS],
	'myTile':					[title: 'myTile for dashboard', d: 'Display \'myTile\'?', ty: sSTR, defa: sFLS],
	'moonPhase':				[title: 'Moon Phase', d: 'Display \'moonPhase\'?', ty: sSTR, defa: sFLS],
	'solarradiation':			[title: 'Solar Radiation', d: 'Display \'solarradiation\'?', ty: sSTR, defa: sFLS],
	'raintoday':				[title: 'Precipitation today (in default unit)', d: 'Display precipitation today?', ty: sNUM, defa: sFLS],
	'percentPrecip':			[title: 'Today\'s Precipitation Probability', d: 'Display today\'s precipitation probability?', ty: sNUM, defa: sFLS],
	'precipExtended':			[title: 'Precipitation Forecast', d: 'Display precipitation forecast?', ty: false, defa: sFLS],
	'obspoll':					[title: 'Observation time', d: 'Display Observation and Poll times?', ty: false, defa: sFLS], 
	'suncalc':					[title: 'Sun calculations', d: 'Display Altitude and Azuimuth of Sun?', ty: false, defa: sFLS],    
	'state':					[title: 'State', d: 'Display \'state\'?', ty: sSTR, defa: sFLS],
	'vis':						[title: 'Visibility (in default unit)', d: 'Display visibility distance?', ty: sNUM, defa: sFLS],
	'weatherSummary':			[title: 'Weather Summary Message', d: 'Display the Weather Summary?', ty: sSTR, defa: sFLS],
	'wind_cardinal':			[title: 'Wind Cardinal', d: 'Display the Wind Direction (text initials)?', ty: sSTR, defa: sFLS],
	'wind_degree':				[title: 'Wind Degree', d: 'Display the Wind Direction (number)?', ty: sNUM, defa: sFLS],
	'wind_direction':			[title: 'Wind direction', d: 'Display the Wind Direction?', ty: sSTR, defa: sFLS],
	'wind_gust':				[title: 'Wind gust (in default unit)', d: 'Display the Wind Gust?', ty: sNUM, defa: sFLS],
	'wind_string':				[title: 'Wind string', d: 'Display the wind string?', ty: sSTR, defa: sFLS],
	'sunMoonAngles':			[title: 'Sun/Moon Angles', d: 'Display calculated Sun & Moon Altitude and Azimuth (numeric and text)?', ty: false, defa: sFLS],
	'moonPhaseDetail':			[title: 'Moon Phase Detail (today/tomorrow/day-after)', d: 'Display per-day Moon Phase value, text, PNG/SVG image and emoji (sourced from OpenWeatherMap), plus the Moon Phase Tile?', ty: false, defa: sFLS],
	'windDirImage':				[title: 'Wind Direction Image', d: 'Display an image or emoji for wind direction, alongside the existing wind_direction/wind_cardinal text?', ty: false, defa: sFLS],
]

// Check Version   ***** with great thanks and acknowledgment to Cobra (CobraVmax) for his original code ****
void updateCheck()
{
	Map paramsUD = [uri: 'https://raw.githubusercontent.com/Scottma61/Hubitat/master/docs/version2.json'] //https://hubitatcommunity.github.io/???/version2.json"]
	asynchttpGet('updateCheckHandler', paramsUD)
}

void updateCheckHandler(resp, data) {
	state.InternalName = 'Weather-Display With OWM-Alerts Forecast Driver'
	Boolean descTextEnable = settings.logInfoEnable ?: false
	if (resp.getStatus() == 200 || resp.getStatus() == 207) {
        Map respUD
		try {
			respUD = parseJson(resp.data)
		} catch (Exception e) {
			logError('updateCheckHandler: failed to parse JSON from version2.json - check the file for syntax errors: ' + e.message)
			return
		}
		// log.warn " Version Checking - Response Data: $respUD"   // Troubleshooting Debug Code - Uncommenting this line should show the JSON response from your webserver
        state.Copyright = respUD.copyright
		// uses reformattted 'version2.json'
		Map driverEntry = respUD?.driver?."${state.InternalName}" as Map
		if (driverEntry == null) {
			logWarn("updateCheck: no entry found in version2.json for driver name '${state.InternalName}' - skipping version check. If you recently renamed this driver, update the key in version2.json to match.")
			return
		}
		String Ver = (String)driverEntry.ver
		String newVer = padVer(Ver)
		String currentVer = padVer(version())
		state.UpdateInfo = driverEntry.updated
        // log.debug 'updateCheck: ${driverEntry.ver}, $state.UpdateInfo, ${respUD.author}'
        switch(newVer) {
            case { it == 'NLS'}:
				state.Status = '<b>** This Driver is no longer supported by ' + respUD.author +'  **</b>'
				if (descTextEnable) log.warn '** This Driver is no longer supported by ${respUD.author} **'
				break
            case { it > currentVer}:
                state.Status = '<b>New Version Available (Version: ' + Ver + ')</b>'
				if (descTextEnable) log.warn '** There is a newer version of this Driver available  (Version: ' + Ver + ') **'
            if (descTextEnable) log.warn '** ' + (String)state.UpdateInfo + ' **'
            break
			case { it < currentVer}:
				state.Status = '<b>You are using a Test version of this Driver (Expecting: ' + Ver + ')</b>'
				if (descTextEnable) log.warn 'You are using a Test version of this Driver (Expecting: ' + Ver + ')'
				break
			default:
                state.Status = 'Current Version: ' + Ver
				if (descTextEnable) log.info 'You are using the current version of this driver'
				break
        }
    }else{
        log.error 'Something went wrong: CHECK THE JSON FILE AND IT\'S URI'
    }
}
/*
	padVer
	Version progression of 1.4.9 to 1.4.10 would mis-compare unless each duple is padded first.
*/
static String padVer(String ver) {
	String pad
    pad = sBLK
	ver.replaceAll( '[vV]', sBLK ).split( /\./ ).each { String it -> pad += it.padLeft( 2, sZERO ) }
	return pad
}
