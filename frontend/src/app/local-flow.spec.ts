import {HttpClient, HTTP_INTERCEPTORS} from '@angular/common/http';
import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';
import {Subject, of} from 'rxjs';
import {JwtInterceptor} from './_interceptors/jwt-interceptor.service';
import {UserService} from './services/user.service';
import {CartComponent} from './pages/cart/cart.component';
import {LoginComponent} from './pages/login/login.component';
import {convertToParamMap} from '@angular/router';

describe('Local API authentication', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({imports: [HttpClientTestingModule], providers: [
      {provide: UserService, useValue: {currentUserValue: {token: 'synthetic-token', type: 'Bearer'}}},
      {provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true}
    ]});
    http = TestBed.get(HttpClient); requests = TestBed.get(HttpTestingController);
  });
  afterEach(() => requests.verify());
  it('attaches JWT to proxied API requests', () => {
    http.get('/api/cart').subscribe();
    const request = requests.expectOne('/api/cart');
    expect(request.request.headers.get('Authorization')).toBe('Bearer synthetic-token');
    request.flush({products: []});
  });
  it('does not attach JWT to external or asset requests', () => {
    ['/assets/demo/lamp.svg', 'https://example.invalid/image'].forEach(url => {
      http.get(url).subscribe(); const request = requests.expectOne(url);
      expect(request.request.headers.has('Authorization')).toBe(false); request.flush({});
    });
  });
  it('does not force JSON content type on multipart uploads', () => {
    http.post('/api/csv/upload', new FormData()).subscribe();
    const request = requests.expectOne('/api/csv/upload');
    expect(request.request.headers.has('Content-Type')).toBe(false); request.flush({});
  });
});

describe('Local shopping interactions', () => {
  let cart: CartComponent;
  let completed: Subject<any>;
  let service: any;
  let router: any;
  beforeEach(() => {
    completed = new Subject<any>(); router = {navigate: jasmine.createSpy('navigate')};
    service = {checkout: jasmine.createSpy('checkout').and.returnValue(completed), update: () => of({count: 2})};
    cart = new CartComponent(service, {currentUser: of({role: 'ROLE_CUSTOMER'})} as any, router);
    cart.productInOrders = [{productId: 'demo-lamp', count: 1, productPrice: 29, productStock: 20} as any];
  });
  afterEach(() => cart.ngOnDestroy());
  it('waits for checkout success before navigating to orders', () => {
    cart.checkout(); expect(router.navigate).not.toHaveBeenCalled();
    completed.next(null); expect(router.navigate).toHaveBeenCalledWith(['/order']);
    expect(cart.productInOrders.length).toBe(0);
  });
  it('retains the cart when checkout fails', () => {
    cart.checkout(); completed.error(new Error('test failure'));
    expect(cart.productInOrders.length).toBe(1); expect(cart.checkingOut).toBe(false);
    expect(router.navigate).not.toHaveBeenCalled(); expect(cart.error).toContain('Checkout failed');
  });
  it('accepts a successful quantity response', () => {
    cart.addOne(cart.productInOrders[0]); expect(cart.productInOrders[0].count).toBe(2);
    expect(cart.pending).toBe(0); expect(cart.total).toBe(58);
  });
  it('blocks checkout while a quantity save is pending', () => {
    cart.pending = 1; cart.checkout(); expect(service.checkout).not.toHaveBeenCalled();
  });
  it('uses a safe catalog destination when returnUrl is absent or external', () => {
    [null, 'https://example.invalid', '//example.invalid'].forEach(returnUrl => {
      const login = new LoginComponent({} as any, router, {snapshot: {queryParamMap: convertToParamMap({returnUrl})}} as any);
      login.ngOnInit(); expect(login.returnUrl).toBe('/product');
    });
  });
});
