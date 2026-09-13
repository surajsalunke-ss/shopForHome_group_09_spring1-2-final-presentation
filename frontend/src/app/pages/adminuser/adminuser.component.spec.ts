import { AppModule } from '../../app.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminuserComponent } from './adminuser.component';

describe('AdminuserComponent', () => {
  let component: AdminuserComponent;
  let fixture: ComponentFixture<AdminuserComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [AppModule, HttpClientTestingModule, RouterTestingModule]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AdminuserComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
