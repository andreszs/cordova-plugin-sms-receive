var exec = require('cordova/exec');

var smsReceive = {
	startWatch: function (successCallback, failureCallback) {
		var onSuccessCallback = function (item) {
			if (typeof (item) == 'object' && typeof (item.body) !== 'undefined') {
				// This is an incoming SMS event
				cordova.fireDocumentEvent('onSMSArrive', {address: item.address, body: item.body, date: item.date});
				successCallback("SMS_RECEIVED");
			} else {
				// This is the result of the regular startWatch method
				successCallback(item);
			}
		};
		exec(onSuccessCallback, failureCallback, 'SMSReceive', 'startWatch', []);
	},
	stopWatch: function (successCallback, failureCallback) {
		exec(successCallback, failureCallback, 'SMSReceive', 'stopWatch', []);
	}
};

module.exports = smsReceive;
