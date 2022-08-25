import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-email',
  templateUrl: './email.component.html',
  styleUrls: ['./email.component.css']
})
export class EmailComponent implements OnInit {

  alert:boolean=false;

  constructor(private https: HttpClient){ }

  ngOnInit() {
  }

  title = 'EmailTemplate';
  dataset: Details = {

    recipient:'',
    subject:'',
    msgBody:''
  };

  dataset1 : Details;



  onSubmit(){
    console.log(this.dataset);
    this.https.post<Details>('http://localhost:8080/sendMail', this.dataset)
    .subscribe(

        data  => {
          this.alert=true;
        },

        res => {
          this.dataset = res;
          this.alert=true;
          console.log(this.dataset);
          console.log('Email Sent successfully');

          this.dataset.recipient = '';
          this.dataset.subject = '';
          this.dataset.msgBody = '';

        });
  }

  closeAlert(){
    this.alert=false;
  }
}
interface Details{

  recipient:string;
  subject:string;
  msgBody:string;
}




