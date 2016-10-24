import 'angular';

import {Auth} from "./appstartup.auth";
import {AuthConfig} from "../models/wellknown/AuthConfig";
import {upgradeAdapter} from "../upgradeAdapter";

// configuration that is obtained from 'well known' endpoints, i.e. on the same
// base url as this app
export class WellKnownConfig {

    private authConfig:AuthConfig;

    private static instance:WellKnownConfig;

    // singleton instance
    public static factory():WellKnownConfig {
        if(WellKnownConfig.instance == null) {
            WellKnownConfig.instance = new WellKnownConfig();
        }
        return WellKnownConfig.instance;
    }

    constructor() {

    }

    public getAuthConfig():AuthConfig {
        return this.authConfig;
    }

    public setAuthConfig(authConfig:AuthConfig) {
        this.authConfig = authConfig;
    }
}

// manually bootstrap the angular app
//
// (NB: do not put an ng-app directive in html anywhere as the auth needs
//      to start and configure before the app starts
//
angular.element(document).ready(($http) => {

    var $injector = angular.injector(['ng']);
    var promise:ng.IQService = $injector.get('$q') as ng.IQService;
    var wellKnownConfig:WellKnownConfig = WellKnownConfig.factory();

    var defer = promise.defer();

    // try to read the auth configuration from local storage, if not found, get it from the public API and store it
    var path: string = 'eds.config.auth';
    var text: string = localStorage.getItem(path);
    if (text === null || typeof text === "undefined" || text === "undefined") {
        // use jQuery to avoid angular http interceptors
        $http.getJSON("/public/wellknown/authconfig", (data:any, textStatus:string, jqXHR:any) => {
            var authConfig = data as AuthConfig;
            localStorage.setItem(path, JSON.stringify(authConfig));
            defer.resolve(authConfig);
        });
    }
    else {
        defer.resolve(<AuthConfig>JSON.parse(text));
    }

    defer.promise.then((authConfig:AuthConfig) => {
        // set the config
        wellKnownConfig.setAuthConfig(authConfig);

        Auth.factory().setOnAuthSuccess(()=> {
            // manually bootstrap angular
            upgradeAdapter.bootstrap(document.body, ['app'], {strictDi: true});
        });

        Auth.factory().setOnAuthError(()=> {
            console.log('Failed to start app as not authenticated, check the well known auth configuration.')
        });

        Auth.factory().init();
    });
});
