var MoovaaScanner = {
    getScan: function (success, failure) {
        cordova.exec(success, failure, "MoovaaScanner", "openScanner", []);
    }
};

module.exports = MoovaaScanner;
