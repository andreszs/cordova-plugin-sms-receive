// For an introduction to the Blank template, see the following documentation:
// http://go.microsoft.com/fwlink/?LinkID=397704
// To debug code on page load in Ripple or on Android devices/emulators: launch your app, set breakpoints, 
// and then run "window.location.reload()" in the JavaScript Console.
(function() {
	"use strict";

	document.addEventListener('deviceready', onDeviceReady.bind(this), false);

	function onDeviceReady() {
		// Handle the Cordova pause and resume events
		document.addEventListener('pause', onPause.bind(this), false);
		document.addEventListener('resume', onResume.bind(this), false);
		// Cordova has been loaded. Perform any initialization that requires Cordova here.
		

		/* Initialize sms-receive plugin */
		if(typeof (SMSReceive) === 'undefined') {
			// Error: plugin not installed
			console.warn('SMSReceive: plugin not present');
			document.getElementById('status').innerHTML = 'Error: The plugin <strong>cordova-plugin-sms-receive</strong> is not present';
		} else {
			// Initialize incoming SMS event listener
			document.addEventListener('onSMSArrive', function(e) {
				console.log('onSMSArrive()');
				var IncomingSMS = e.data;
				console.log('sms.address:' + IncomingSMS.address);
				console.log('sms.body:' + IncomingSMS.body);
				// Debug received SMS contents as JSON
				document.getElementById('event').innerHTML = 'SMS from: ' + IncomingSMS.address + '<br />Service Center: ' + IncomingSMS.service_center + '<br />Received on: ' + IncomingSMS.date + '<br />Body: ' + IncomingSMS.body;
			});

			// Bind Start Watch method to button 1
			document.getElementById('startWatch').addEventListener('click', function() {
				SMSReceive.startWatch(function() {
					document.getElementById('status').innerHTML = 'SMS Watching started';
				}, function() {
					document.getElementById('status').innerHTML = 'Plugin failed to start watching';
				});
			});

			// Bind Stop Watch method to button 2
			document.getElementById('stopWatch').addEventListener('click', function() {
				SMSReceive.stopWatch(function() {
					document.getElementById('status').innerHTML = 'SMS Watching stopped';
				}, function() {
					document.getElementById('status').innerHTML = 'Plugin failed to stop watching';
				});
			});
		}
	};

	function onPause() {
		// TODO: This application has been suspended. Save application state here.
	};

	function onResume() {
		// TODO: This application has been reactivated. Restore application state here.
	};
})();