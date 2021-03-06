import {linq, LoggerService} from "eds-common-js";
import {StateService} from "ui-router-ng2";
import {Component} from "@angular/core";
import {Subscription} from "rxjs/Subscription";
import {ServiceService} from "../services/service.service";
import {RabbitService} from "../queueing/rabbit.service";
import {Routing} from "../queueing/Routing";
import {ServiceListComponent} from "../services/serviceList.component";
import {RabbitNode} from "../queueing/models/RabbitNode";
import {Service} from "../services/models/Service";
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {ScheduledTasksService} from "./scheduledTasks.service";
import {ScheduledTaskAudit} from "./models/ScheduledTaskAudit";
import {ScheduledTaskRule} from "./models/ScheduledTaskRule";
import {ScheduledTaskHistoryDialog} from "./scheduledTaskHistory.dialog";
import {DateTimeFormatter} from "../utility/DateTimeFormatter";

@Component({
    template : require('./scheduledTasks.html')
})
export class ScheduledTasksComponent {

    //SD-338 - need to import the static formatting functions so they can be used by the HTML template
    formatYYYYMMDDHHMM = DateTimeFormatter.formatYYYYMMDDHHMM;
    formatHHMMSS = DateTimeFormatter.formatHHMMSS;

    latest: ScheduledTaskAudit[];
    rules: ScheduledTaskRule[];
    statusLastRefreshed: Date;

    constructor(private $modal:NgbModal,
                private serviceService:ServiceService,
                private scheduledTasksService:ScheduledTasksService,
                private rabbitService:RabbitService,
                private logger:LoggerService,
                private $state:StateService) {


    }

    ngOnInit() {
        var vm = this;
        vm.refreshAll();
    }

    refreshAll() {
        var vm = this;
        vm.statusLastRefreshed = new Date();
        vm.refreshRules();
        vm.refreshSummary();
    }


    refreshRules() {
        var vm = this;
        vm.scheduledTasksService.getScheduledTaskRules()
            .subscribe(
                data => {
                    vm.rules = data;
                },
                (error) => {
                    vm.logger.error('Failed to load rules', error);
                }
            );

    }


    refreshSummary() {

        var vm = this;
        vm.scheduledTasksService.getScheduledTaskSummary()
            .subscribe(
                data => {
                    vm.latest = data;
                },
                (error) => {
                    vm.logger.error('Failed to load summary', error);
                }
            );
    }

    getSummariesToDisplay(): ScheduledTaskAudit[] {
        var vm = this;

        var ret:ScheduledTaskAudit[] = [];

        //console.log('latest = ' + vm.latest)
        if (vm.latest) {
            for (var i=0; i<vm.latest.length; i++) {
                var n = vm.latest[i];
                //console.log('adding latest at ' + i);
                ret.push(n);
            }
        }

        if (vm.rules) {

            for (var i=0; i<vm.rules.length; i++) {
                var rule = vm.rules[i];
                var found = false;

                for (var j=0; j<ret.length; j++) {
                    var n = ret[j];
                    if (n.applicationName == rule.applicationName
                        && n.taskName == rule.taskName) {

                        //set the rule on the task
                        n.rule = rule;
                        found = true;
                    }
                }

                if (!found) {
                    //console.log('Did not find ' + rule.applicationName + ' for ' + rule.taskName);
                    var n:ScheduledTaskAudit = new ScheduledTaskAudit();
                    n.applicationName = rule.applicationName;
                    n.taskName = rule.taskName;
                    n.rule = rule;
                    ret.push(n);
                }
            }
        }

        //sort
        ret = linq(ret).OrderBy(s => s.applicationName.toLowerCase()).ToArray();

        return ret;
    }

    isItemTooOld(audit: ScheduledTaskAudit): boolean {
        var vm = this;

        //if this task has no rule, return false
        if (!audit.rule
            || !audit.rule.withinUnit
            || !audit.rule.within) {
            return false;
        }

        //if never run, then definitely is too old
        if (!audit.timestamp) {
            return true;
        }

        var threshold = vm.getRuleTimePeriod(audit);
        var warningTime = vm.statusLastRefreshed.getTime() - threshold;
        return audit.timestamp < warningTime;
    }

    private getRuleTimePeriod(audit: ScheduledTaskAudit): number {
        var rule = audit.rule;
        if (rule.withinUnit == 'minute') {
            return 1000 * 60 * rule.within;

        } else if (rule.withinUnit == 'hour') {
            return 1000 * 60 * 60 * rule.within;

        } else if (rule.withinUnit == 'day') {
            return 1000 * 60 * 60 * 24 * rule.within;

        } else if (rule.withinUnit == 'week') {
            return 1000 * 60 * 60 * 24 * 7 * rule.within;

        } else if (rule.withinUnit == 'month') {
            return 1000 * 60 * 60 * 24 * 30 * rule.within; //just approx month length

        } else if (rule.withinUnit == 'year') {
            return 1000 * 60 * 60 * 24 * 365 * rule.within;

        } else {
            console.log('unexpected rule unit [' + rule.withinUnit + ']');
            return 0;
        }
    }

    getRuleWithinDesc(audit: ScheduledTaskAudit): string {
        var vm = this;

        if (!audit.rule
            || !audit.rule.withinUnit
            || !audit.rule.within) {
            return null;
        }

        var ret = 'Should be run every ' + audit.rule.within + ' ' + audit.rule.withinUnit;
        if (audit.rule.within > 1) {
            ret += 's';
        }
        return ret;
    }

    getItemDueIn(audit: ScheduledTaskAudit): string {
        var vm = this;

        //if no rule about how often to run it, then return an empty String
        if (!audit.rule
            || !audit.rule.withinUnit
            || !audit.rule.within) {
            return '';
        }

        //if never been run, then it's due NOW
        if (!audit.timestamp) {
            return 'Now';
        }

        //the "due in" is the difference between NOW and when it should be next run, based on the rule
        //and when it was last run
        var now = vm.statusLastRefreshed;

        var ruleMillis = vm.getRuleTimePeriod(audit);
        var nextDueMillis = audit.timestamp + ruleMillis;

        return DateTimeFormatter.getDateDiffDesc(now, new Date(nextDueMillis), 2);
    }


    viewHistory(audit: ScheduledTaskAudit) {
        var vm = this;
        ScheduledTaskHistoryDialog.open(vm.$modal, audit);
    }
}