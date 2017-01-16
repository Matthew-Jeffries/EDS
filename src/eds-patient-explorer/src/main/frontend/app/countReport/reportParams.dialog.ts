import {Component, Input, OnInit} from "@angular/core";
import {NgbModal, NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {ReportParams} from "./models/ReportParams";
import moment = require("moment");
import {CodePickerDialog} from "../coding/codePicker.dialog";
import {CodeSetValue} from "../coding/models/CodeSetValue";
import {CodingService} from "../coding/coding.service";

@Component({
    selector: 'ngbd-modal-content',
    template: require('./reportParams.html')
})
export class ReportParamsDialog implements OnInit {
    @Input() query;
    runDate : Date;
    originalCode : string;
    valueMax : number;
    valueMin : number;
    snomedCode : CodeSetValue;

    public static open(modalService: NgbModal, query: string) {
        const modalRef = modalService.open(ReportParamsDialog, {backdrop: "static", size: 'lg'});
        modalRef.componentInstance.query = query;
        return modalRef;
    }

    constructor(protected modalService : NgbModal, protected activeModal: NgbActiveModal, protected codingService : CodingService ) {
    }

    ngOnInit(): void {
        // work out prompts from query text
        this.runDate = new Date();

        if (this.query.indexOf(':SnomedCode') >= 0) this.snomedCode = null;
        if (this.query.indexOf(':OriginalCode') >= 0) this.originalCode = null;
        if (this.query.indexOf(':ValueMin') >= 0) this.valueMin= null;
        if (this.query.indexOf(':ValueMax') >= 0) this.valueMax= null;
    }

    selectSnomed() {
        var vm = this;
        CodePickerDialog.open(vm.modalService, [], true)
          .result.then(
          (result) => {
              vm.snomedCode = result[0];
              vm.snomedCode.term = 'Loading...';
              vm.codingService.getPreferredTerm(vm.snomedCode.code)
                .subscribe(
                  (term) => vm.snomedCode.term = term.preferredTerm
                );
          }
        )
    }

    hide(item : any) {
        return typeof(item) === 'undefined';
    }

    ok() {
        let params : any = {};

        params.RunDate = "'" + moment(this.runDate).format('DD/MM/YYYY') + "'";
        params.SnomedCode = (this.snomedCode) ? this.snomedCode.code : 'null';
        params.OriginalCode = (this.originalCode) ? "'" + this.originalCode + "'" : 'null';
        params.ValueMin = (this.valueMin) ? this.valueMin : 'null';
        params.ValueMax = (this.valueMax) ? this.valueMax  : 'null';

        this.activeModal.close(params);
        console.log('OK Pressed');
    }

    cancel() {
        this.activeModal.dismiss('cancel');
        console.log('Cancel Pressed');
    }
}
