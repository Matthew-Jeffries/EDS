import {Component} from "@angular/core";
import {FolderNode} from "../folder/models/FolderNode";
import {ItemSummaryList} from "../library/models/ItemSummaryList";
import {FolderItem} from "../folder/models/FolderContent";
import {LibraryService} from "../library/library.service";
import {LibraryItem} from "../library/models/LibraryItem";
import {LoggerService} from "../common/logger.service";
import {CountReportService} from "./countReport.service";
import {ReportParams} from "./models/ReportParams";
import {ReportParamsDialog} from "./reportParams.dialog";
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {Enumerable} from "linqts/dist/linq";
import {ItemType} from "../folder/models/ItemType";

@Component({
	template : require('./countReports.html')
})
export class CountReportComponent {
	selectedFolder : FolderNode;
	itemSummaryList : ItemSummaryList;
	selectedItem : FolderItem;
	libraryItem : LibraryItem;

	constructor(
		protected $modal : NgbModal,
		protected logger : LoggerService,
		protected countReportService : CountReportService,
		protected libraryService : LibraryService) {
	}

	folderChanged($event) {
		this.selectedFolder = $event.selectedFolder;
		this.refresh();
	}

	refresh() {
		var vm = this;
		vm.libraryService.getFolderContents(vm.selectedFolder.uuid)
			.subscribe(
				(data) => {
					vm.itemSummaryList = data;
					vm.selectedFolder.loading = false;
				});
	}

	getSummaryList() {
		if (!this.itemSummaryList || !this.itemSummaryList.contents)
			return null;

		return this.itemSummaryList.contents
			.filter(item => item.type == ItemType.CountReport);
	}

	selectRow(item : FolderItem) {
		if (this.selectedItem == item)
			return;

		this.selectedItem = item;
		var vm = this;
		vm.libraryService.getLibraryItem(item.uuid)
			.subscribe(
				(libraryItem) => vm.libraryItem = libraryItem,
				(error) => vm.logger.error('Error loading', error, 'Error')
			);
	}

	runReport() {
		var vm = this;
		// Get param list from query
		ReportParamsDialog.open(vm.$modal, this.libraryItem.countReport.query)
			.result.then(function (params) {
			vm.executeReport(params);
		});
	}

	executeReport(params : Map<string, string>) {
		var vm = this;
		vm.countReportService.runReport(this.libraryItem.uuid, params)
			.subscribe(
				(result) => {
					vm.logger.success('Report successfully run', result, 'Run report');
					vm.libraryItem.countReport = result.countReport;
				},
				(error) => vm.logger.error('Error running report', error, 'Error')
			);
	}

	exportNHSNumber() {
		var vm = this;
		let reportId = this.libraryItem.uuid;
		vm.countReportService.exportNHSNumbers(reportId)
			.subscribe(
				(result) => {
					vm.logger.success('NHS numbers successfully exported', reportId, 'Export NHS numbers');
					let filename = 'Report_'+reportId+'_NHS.csv';
					vm.downloadFile(filename, result)
				},
				(error) => vm.logger.error('Error exporting HNS numbers', error, 'Error')
			);
	}

	exportData() {
		var vm = this;
		let reportId = this.libraryItem.uuid;
		vm.countReportService.exportData(reportId)
			.subscribe(
			(result) => {
				vm.logger.success('Data successfully exported', reportId, 'Export data');
				let filename = 'Report_'+reportId+'_Dat.csv';
				vm.downloadFile(filename, result)
			},
			(error) => vm.logger.error('Error exporting data', error, 'Error')
		);
	}

	downloadFile(filename : string, data: string){
		var blob = new Blob([data], { type: 'text/plain' });
		window['saveAs'](blob, filename);
	}
}
