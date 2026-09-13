import { RouterTestingModule } from '@angular/router/testing';
import { CookieService } from 'ngx-cookie-service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { WishListService } from './wish-list.service';

describe('WishListService', () => {
  beforeEach(() => TestBed.configureTestingModule({imports: [HttpClientTestingModule, RouterTestingModule], providers: [CookieService]}));

  it('should be created', () => {
    const service: WishListService = TestBed.get(WishListService);
    expect(service).toBeTruthy();
  });
});
