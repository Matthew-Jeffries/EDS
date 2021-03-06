import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {linq, LoggerService} from "eds-common-js";
import {StateService} from "ui-router-ng2";
import {Component} from "@angular/core";
import {Subscription} from "rxjs/Subscription";
import {ServiceService} from "../services/service.service";
import {SubscribersService} from "./subscribers.service";
import {OdsSearchDialog} from "../services/odsSearch.dialog";
import {ServiceListComponent} from "../services/serviceList.component";
import {QueueReaderStatusService} from "../queueReaderStatus/queueReaderStatus.service";
import {QueueReaderStatus} from "../queueReaderStatus/queueReaderStatus";
import {SubscriberConfiguration} from "./models/SubscriberConfiguration";
import {SubscriberDetailComponent} from "./subscriberDetail.component";
import {DateTimeFormatter} from "../utility/DateTimeFormatter";

@Component({
    template : require('./subscribers.html')
})
export class SubscribersComponent {

    //SD-338 - need to import the static formatting functions so they can be used by the HTML template
    formatHHMMSS = DateTimeFormatter.formatHHMMSS;

    //subscriber status
    subscribers: SubscriberConfiguration[];
    statusesLastRefreshed: Date;
    refreshingStatus: boolean;

    /*configurations: SubscribersConfiguration[];
    refreshingStatusMap: {};
    statusMap: {};
    statusesLastRefreshed: Date;*/


    constructor(private $modal: NgbModal,
                protected subscribersService: SubscribersService,
                private queueReaderStatusService: QueueReaderStatusService,
                protected logger: LoggerService,
                protected $state: StateService) {


    }

    ngOnInit() {
        var vm = this;

        vm.refreshScreen();
    }

    refreshScreen() {
        var vm = this;
        vm.refreshSubscribers();
    }


    refreshSubscribers() {
        var vm = this;
        vm.statusesLastRefreshed = new Date();
        vm.refreshingStatus = true;

        vm.subscribersService.getSubscribersInstances().subscribe(
            (result) => {
                vm.subscribers = linq(result).OrderBy(s => s.name).ToArray();
                vm.refreshingStatus = false;
            },
            (error) => {
                vm.logger.error('Failed get subscriber details', error);
                vm.refreshingStatus = false;
            }
        )
    }

    getSubscribersToDisplay(): SubscriberConfiguration[] {
        var vm = this;
        if (!vm.subscribers) { //if not retrieved yet
            return vm.subscribers;
        }

        var ret = [];
        var i;
        for (i=0; i<vm.subscribers.length; i++) {
            var subscriber = vm.subscribers[i];
            //console.log('checking configuration at ' + i);
            //console.log(configuration);

            //TODO - filtering on subscriber feeds
            /*if (vm.subscribersService.filterInstanceName) {
                var configInstanceName = configuration.instanceName;
                if (vm.subscribersService.filterInstanceName != configInstanceName) {
                    continue;
                }
            }

            if (vm.subscribersService.showWarningsOnly) {

                //always include configurations until we've got data back for them
                if (!vm.isRefreshing(configuration)) {

                    var configurationId = configuration.configurationId;
                    var status = vm.statusMap[configurationId];
                    if (!status.warning) { //if the warning boolean is false, skip it
                        continue;
                    }
                }
            }*/

            ret.push(subscriber);
        }
        return ret;
    }

    /*refreshStatuses() {
        var vm = this;
        vm.statusesLastRefreshed = new Date();

        for (var i = 0; i < vm.configurations.length; i++) {
            var configuration = vm.configurations[i];
            vm.refreshStatus(configuration);
        }
    }

    refreshStatus(configuration: SubscribersConfiguration) {
        var vm = this;

        var configurationId = configuration.configurationId;

        vm.refreshingStatusMap[configurationId] = true;

        vm.subscribersService.getSubscribersStatus(configurationId).subscribe(
            (result) => {

                vm.calculateErrors(result); //must do this before the below fn
                vm.calculateIfWarning(result);
                vm.statusMap[configurationId] = result;
                vm.refreshingStatusMap[configurationId] = false;
            },
            (error) => {

                vm.refreshingStatusMap[configurationId] = false;
                vm.logger.error('Failed to get status for ' + configurationId, error);
            }
        )
    }*/

    /**
     * filters the batches to work out which are OK and which are in error
     */
    /*calculateErrors(status: SubscribersChannelStatus) {
        var vm = this;

        var ok = [];
        var dpaError = [];
        var error = [];

        //if not set, just set to an empty array
        if (!status.completeBatchContents) {
            status.completeBatchContents = [];
        }

        for (var i=0; i<status.completeBatchContents.length; i++) {
            var content = status.completeBatchContents[i];
            if (content.notified) {
                ok.push(content);
            } else {
                var errorMsg = content.error;
                if (errorMsg
                    && (errorMsg.indexOf('No DPA found') > -1
                        || errorMsg.indexOf('no DPA exists') > -1)) {
                    dpaError.push(content);
                } else {
                    error.push(content);
                }
            }
        }

        status.okBatches = ok;
        status.dpaErrorBatches = dpaError;
        status.errorBatches = error;
    }

    calculateIfWarning(status: SubscribersChannelStatus) {
        var vm = this;

        //any of these count as a warning
        if (vm.isLastPollAttemptTooOld(status)
            || !status.latestPollingStart
            || status.latestPollingException
            || vm.isLastExtractTooOld(status)
            || !status.latestBatchId
            || status.errorBatches.length > 0
            || status.dpaErrorBatches.length > 0) {

            status.warning = true;
        }
    }

    isRefreshing(configuration: SubscribersConfiguration): boolean {
        var vm = this;

        var configurationId = configuration.configurationId;
        return vm.refreshingStatusMap[configurationId];
    }*/

    /**
     * returns the status for a configuration. Note this uses an array so that we can use the angular
     * for loop, and avoid having to call this function dozens of times.
     */
    /*getStatusToDisplay(configuration: SubscribersConfiguration): SubscribersChannelStatus[] {
        var vm = this;

        var ret = [];

        var configurationId = configuration.configurationId;
        var status = vm.statusMap[configurationId];
        if (status) {
            ret.push(status);
        }

        return ret;
    }

    */

    /*filterOrgs(arr: SubscribersBatchContents[], wantOk: boolean): SubscribersBatchContents[] {
        var ret = [];

        var i;
        for (i=0; i<arr.length; i++) {
            var c = arr[i];
            if (c.notified == wantOk) {
                ret.push(c);
            }
        }
        return ret;
    }*/

    /*getPanelClass(status: SubscribersChannelStatus): string {
        if (status.instanceName) {
            return "panel panel-primary";
        } else {
            return "panel panel-info";
        }
    }*/


    /*isLastPollAttemptTooOld(status: SubscribersChannelStatus): boolean {
        var vm = this;

        var lastPolled = status.latestPollingStart;
        if (!lastPolled) {
            return true;
        }
        var pollingFrequencySec = status.pollFrequencySeconds;
        var pollingFrequencyMs = pollingFrequencySec * 1000;
        pollingFrequencyMs = (pollingFrequencyMs * 2); //double the polling frequency to give a fair window

        //var now = new Date();
        var now = vm.statusesLastRefreshed; //use date of refresh rather than current date so it doesn't change if you don't refresh the page
        var lastPolledDiffMs = now.getTime() - lastPolled;

        return lastPolledDiffMs > pollingFrequencyMs;
    }*/

    /*isLastExtractTooOld(status: SubscribersChannelStatus): boolean {
        var vm = this;

        var lastExtract = status.latestBatchReceived;
        if (!lastExtract) {
            return true;
        }

        var dataFrequencyDays = status.dataFrequencyDays;
        var dataFrequencyMs = dataFrequencyDays * 24 * 60 * 60 * 1000;
        dataFrequencyMs = (dataFrequencyMs * 1.5); //add 10% to the poling frequency so it's less touchy

        //var now = new Date();
        var now = vm.statusesLastRefreshed; //use date of refresh rather than current date so it doesn't change if you don't refresh the page
        var lastExtractDiffMs = now.getTime() - lastExtract;

        return lastExtractDiffMs > dataFrequencyMs;
    }*/

    viewHistory(configuration: SubscriberConfiguration) {
        var vm = this;
//        SubscribersHistoryDialog.open(vm.$modal, configuration);
    }



    /*getInstanceNames(): string[] {
        var vm = this;

        var ret = [];

        if (vm.configurations) {
            for (var i=0; i<vm.configurations.length; i++) {
                var configuration = vm.configurations[i];
                var instanceName = configuration.instanceName;
                if (ret.indexOf(instanceName) == -1) {
                    ret.push(instanceName);
                }
            }
        }

        ret.sort();

        return ret;
    }*/


    getSubscriberLocationDesc(sub: SubscriberConfiguration): string {
        if (!sub.subscriberLocation) {
            return '?'
        } else if (sub.subscriberLocation == 'Internal') {
            return 'I';
        } else if (sub.subscriberLocation == 'Remote') {
            return 'R';
        } else if (sub.subscriberLocation == 'InternalAndRemote') {
            return 'IR';
        } else {
            return 'Unknown location ' + sub.subscriberLocation;
        }
    }

    getSubscriberSchemaDesc(sub: SubscriberConfiguration): string {
        if (!sub.schema) {
            return '?'
        } else if (sub.schema == 'CompassV1') {
            return 'v1';
        } else if (sub.schema == 'CompassV2') {
            return 'v2';
        } else {
            return 'Unknown schema ' + sub.schema;
        }
    }

    getRemoteSubscriberIdDesc(sub: SubscriberConfiguration): string {
        if (!sub.remoteSubscriberId) {
            return null
        } else {
            return 'Remote subscriber ID ' + sub.remoteSubscriberId;
        }
    }

    viewSubscriberDetails(sub: SubscriberConfiguration) {
        var vm = this;

        var subscriberName = sub.name;
        console.log('view detail for ' + subscriberName);

        vm.$state.go('app.subscriberDetail', {subscriberName: subscriberName});
    }


    /**
     * saves current list to CSV
     */
    saveToCsv() {

        var vm = this;

        //create CSV content in a String
        var lines = [];
        var line;

        line = '\"Subscriber Name\",' +
            '\"Description\",' +
            '\"Location\",' +
            '\"Remote Subscriber ID\",' +
            '\"Schema\",' +
            '\"PI\",' +
            '\"Excludes Test Patients\",' +
            '\"Excludes Blank NHS Number Patients\",' +
            '\"Exclude NHS Number Regex\",' +
            '\"Cohort\",' +
            '\"Subscriber DB\",' +
            '\"Subscriber DB Name\",' +
            '\"Subscriber Transform DB\",' +
            '\"Subscriber Transform DB Name\",' +
            '\"Publishers\",' +
            '\"In Up-to-date\",' +
            '\"In 1d Behind\",' +
            '\"In 2d+ Behind\",' +
            '\"Out Up-to-date\",' +
            '\"Out 1d Behind\",' +
            '\"Out 2d+ Behind\"';
        lines.push(line);

        var subs = vm.getSubscribersToDisplay();
        for (var i=0; i<subs.length; i++) {
            var config = subs[i] as SubscriberConfiguration;

            var cols = [];

            cols.push(config.name);
            cols.push(config.description);
            cols.push(vm.getSubscriberLocationDesc(config));
            if (config.remoteSubscriberId) {
                cols.push(config.remoteSubscriberId);
            } else {
                cols.push('');
            }
            cols.push(vm.getSubscriberSchemaDesc(config));
            if (config.deidentified) {
                cols.push('N');
            } else {
                cols.push('Y');
            }
            if (config.excludeTestPatients) {
                cols.push('Y');
            } else {
                cols.push('N');
            }
            if (config.excludePatientsWithoutNhsNumber) {
                cols.push('Y');
            } else {
                cols.push('N');
            }
            if (config.excludeNhsNumberRegex) {
                cols.push(config.excludeNhsNumberRegex);
            } else {
                cols.push('');
            }
            cols.push(config.cohortDesc);
            if (config.subscriberDatabase) {
                cols.push(config.subscriberDatabase);
            } else {
                cols.push('');
            }
            if (config.subscriberDatabaseName) {
                cols.push(config.subscriberDatabaseName);
            } else {
                cols.push('');
            }
            cols.push(config.subscriberTransformDatabase);
            cols.push(config.subscriberTransformDatabaseName);
            cols.push(config.numPublishers);
            cols.push(config.inboundUpToDate);
            cols.push(config.inboundOneDay);
            cols.push(config.inboundMoreDays);
            cols.push(config.outboundUpToDate);
            cols.push(config.outboundOneDay);
            cols.push(config.outboundMoreDays);

            line = '\"' + cols.join('\",\"') + '\"';
            lines.push(line);
        }

        var csvStr = lines.join('\r\n');

        const filename = 'subscriber_status.csv';
        const blob = new Blob([csvStr], { type: 'text/plain' });

        let url = window.URL.createObjectURL(blob);
        let a = document.createElement('a');
        document.body.appendChild(a);
        a.setAttribute('style', 'display: none');
        a.href = url;
        a.download = filename;
        a.click();
        window.URL.revokeObjectURL(url);
        a.remove();
    }


}