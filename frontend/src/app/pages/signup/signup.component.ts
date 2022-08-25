import {Component, OnInit} from '@angular/core';
import {Location} from '@angular/common';
import {User} from "../../models/User";
import {UserService} from "../../services/user.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css']
})
export class SignupComponent implements OnInit {

  mobNumberPattern = "^((\\+91-?)|0)?[0-9]{10}$";
  EmailPattern = "^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,4}$";
  NamePattern = "[a-zA-Z][a-zA-Z ]*";
  user: User;

  constructor( private location: Location,
               private userService: UserService,
               private router: Router) {
    this.user = new User();

  }



  ngOnInit() {


  }
  onSubmit() {
    this.userService.signUp(this.user).subscribe(u => {
      this.router.navigate(['/login']);
    },
        e => {});
  }

}
