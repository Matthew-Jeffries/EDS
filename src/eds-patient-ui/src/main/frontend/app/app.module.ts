/// <reference path="../typings/index.d.ts"/>
// Node dependencies
import 'angular-ui-bootstrap';
import 'angular-ui-router';
import 'angular-toastr';
import 'bootstrap';

// Internal dependencies
import '../content/css/index.css';

// Internal module dependencies
import "./appstartup/appstartup.module";
import "./audit/audit.module";
import "./blocks/blocks.module";
import "./consent/consent.module";
import "./core/core.module";
import "./dialogs/dialogs.module";
import "./layout/layout.module";
import "./medicalRecord/medicalRecord.module";
import "./models/models.module";

// Node module types
import {StateService} from "angular-ui-router";
import IModalService = angular.ui.bootstrap.IModalService;
import IRootScopeService = angular.IRootScopeService;

// Internal module types
import {AppRoute} from "./app.route";
import {AdminService} from "./core/admin.service";
import {SecurityService} from "./core/security.service";

export let app = angular.module('app', [
		'app.models',
		'app.core',
		'app.blocks',
		'app.layout',
		'app.dialogs',

		'app.medicalRecord',
		'app.consent',
		'app.audit'
	])
	.run(['$state', '$rootScope', 'AdminService', 'SecurityService', /*'EdsLoggerService',*/ '$uibModal',
		function ($state:StateService,
							$rootScope:IRootScopeService,
							adminService:AdminService,
							securityService:SecurityService,
							// logger:EdsLoggerService,
							$modal:IModalService) {


			$rootScope.$on('$stateChangeStart', function (event, toState, toParams, fromState, fromParams) {
				if (toState.unsecured !== true && !securityService.isAuthenticated()) {
					var data = {
						isAuth : securityService.isAuthenticated(),
						toState : toState
					};
					// logger.log('You are not logged in', data);
					event.preventDefault();
					//$state.transitionTo('app.register');		// TODO: create registration controller
				}
				if (adminService.getPendingChanges()) {
					event.preventDefault();
					var options = {
						templateUrl:'app/dialogs/messageBox/messageBox.html',
						controller:'MessageBoxController',
						controllerAs:'ctrl',
						backdrop:'static',
						resolve: {
							title : () => 'Unsaved changes',
							message : () => 'There are unsaved changes, do you wish to continue',
							okText : () => 'Yes',
							cancelText : () => 'No'
						}
					};

					$modal.open(options)
						.result
						.then(function() {
							adminService.clearPendingChanges();
							$state.transitionTo(toState);
						});
				}
			});

			// logger.log('Starting app...', securityService.getCurrentUser());
			$state.go('app.medicalRecord', {}, {});
		}]
	)
	.config(AppRoute);
