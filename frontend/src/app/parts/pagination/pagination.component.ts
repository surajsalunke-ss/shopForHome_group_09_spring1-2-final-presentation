import {Component, Input, OnInit} from '@angular/core';
import { Role } from 'src/app/enum/Role';
import { JwtResponse } from 'src/app/response/JwtResponse';

@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.component.html',
  styleUrls: ['./pagination.component.css']
})
export class PaginationComponent implements OnInit {

  currentUser: JwtResponse;
  Role = Role;

  @Input() currentPage: any;

  constructor() {
  }

  ngOnInit() {
  }

  counter(i = 1) {
    return new Array(i);
  }
}
