import { Injectable } from '@angular/core';
import * as FileSaver from 'file-saver';
// Angular 7/Webpack 4 cannot execute the package's newer .mjs entry.
import * as XLSX from 'xlsx/dist/xlsx.full.min';
import { WorkSheet, WorkBook } from 'xlsx';

const EXCEL_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8';
const EXCEL_EXTENSION = '.xlsx';

@Injectable()
export class ExcelService {

  constructor() { }

  public exportAsExcelFile(json: any[], excelFileName: string): void {
    
    const myworksheet: WorkSheet = XLSX.utils.json_to_sheet(json);
    const myworkbook: WorkBook = { Sheets: { 'data': myworksheet }, SheetNames: ['data'] };
    const excelBuffer: any = XLSX.write(myworkbook, { bookType: 'xlsx', type: 'array' });
    this.saveAsExcelFile(excelBuffer, excelFileName);
  }

  private saveAsExcelFile(buffer: any, fileName: string): void {
    const data: Blob = new Blob([buffer], {
      type: EXCEL_TYPE
    });
    FileSaver.saveAs(data, fileName + '_exported'+ EXCEL_EXTENSION);
  }

}