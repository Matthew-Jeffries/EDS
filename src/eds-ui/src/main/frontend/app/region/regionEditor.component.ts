import {Component} from "@angular/core";
import {Organisation} from "../organisationManager/models/Organisation";
import {Service} from "../services/models/Service";
import {AdminService} from "../administration/admin.service";
import {RegionService} from "./region.service";
import {LoggerService} from "../common/logger.service";
import {Transition, StateService} from "ui-router-ng2";
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {Region} from "./models/Region";
import {OrganisationManagerPickerDialog} from "../organisationManager/organisationManagerPicker.dialog";

@Component({
    template: require('./regionEditor.html')
})
export class RegionEditorComponent {

    region : Region = <Region>{};
    organisations : Organisation[];

    constructor(private $modal: NgbModal,
                private state : StateService,
                private log:LoggerService,
                private adminService : AdminService,
                private regionService : RegionService,
                private transition : Transition
    ) {
        this.performAction(transition.params()['itemAction'], transition.params()['itemUuid']);
    }

    protected performAction(action:string, itemUuid:string) {
        switch (action) {
            case 'add':
                this.create(itemUuid);
                break;
            case 'edit':
                this.load(itemUuid);
                break;
        }
    }

    create(uuid : string) {
        this.region = {
            name : ''
        } as Region;
    }

    load(uuid : string) {
        var vm = this;
        vm.regionService.getRegion(uuid)
            .subscribe(result =>  {
                    vm.region = result;
                    vm.getRegionOrganisations();
                },
                error => vm.log.error('Error loading', error, 'Error')
            );
    }

    save(close : boolean) {
        var vm = this;

        // Populate organisations before save

        vm.region.organisations = {};
        for (var idx in this.organisations) {
            var organisation : Organisation = this.organisations[idx];
            this.region.organisations[organisation.uuid] = organisation.name;
        }


        vm.regionService.saveRegion(vm.region)
            .subscribe(saved => {
                    vm.adminService.clearPendingChanges();
                    vm.log.success('Item saved', vm.region, 'Saved');
                    if (close) { vm.state.go(vm.transition.from()); }
                },
                error => vm.log.error('Error saving', error, 'Error')
            );
    }

    close() {
        this.adminService.clearPendingChanges();
        this.state.go(this.transition.from());
    }

    private getRegionOrganisations() {
        var vm = this;
        vm.regionService.getRegionOrganisations(vm.region.uuid)
            .subscribe(
                result => vm.organisations = result,
                error => vm.log.error('Failed to load region organisations', error, 'Load region organisation')
            );
    }

    private editOrganisations() {
        var vm = this;
        console.log("Calling the picker");
        OrganisationManagerPickerDialog.open(vm.$modal, vm.organisations)
            .result.then(function (result : Organisation[]) {
            vm.organisations = result;
        });
    }
}
