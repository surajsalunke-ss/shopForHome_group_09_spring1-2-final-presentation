import {Component, OnDestroy, OnInit} from '@angular/core';
import {CartService} from '../../services/cart.service';
import {UserService} from '../../services/user.service';
import {JwtResponse} from '../../response/JwtResponse';
import {ProductInOrder} from '../../models/ProductInOrder';
import {Router} from '@angular/router';
import {Role} from '../../enum/Role';
import {Subscription} from 'rxjs';

@Component({selector: 'app-cart', templateUrl: './cart.component.html', styleUrls: ['./cart.component.css']})
export class CartComponent implements OnInit, OnDestroy {
    productInOrders: ProductInOrder[] = [];
    currentUser: JwtResponse;
    userSubscription: Subscription;
    pending = 0;
    checkingOut = false;
    error = '';

    constructor(private cartService: CartService, private userService: UserService, private router: Router) {
        this.userSubscription = userService.currentUser.subscribe(user => this.currentUser = user);
    }
    get total() { return this.productInOrders.reduce((sum, item) => sum + item.count * item.productPrice, 0); }
    ngOnInit() { this.reload(); }
    ngOnDestroy() { this.userSubscription.unsubscribe(); }
    reload() { this.cartService.getCart().subscribe(items => this.productInOrders = items); }

    static validateCount(item) {
        item.count = Math.max(1, Math.min(item.productStock, Math.floor(Number(item.count) || 1)));
    }
    onChange(item) {
        CartComponent.validateCount(item);
        this.error = '';
        if (!this.currentUser) { this.cartService.storeLocalCart(); return; }
        this.pending++;
        this.cartService.update(item).subscribe(updated => {
            Object.assign(item, updated); this.pending--;
        }, () => { this.pending--; this.error = 'Quantity could not be saved. Please try again.'; this.reload(); });
    }
    addOne(item) { item.count++; this.onChange(item); }
    minusOne(item) { item.count--; this.onChange(item); }
    remove(item) {
        this.pending++;
        this.cartService.remove(item).subscribe(() => {
            this.productInOrders = this.productInOrders.filter(p => p.productId !== item.productId);
            if (!this.currentUser) this.cartService.storeLocalCart();
            this.pending--;
        }, () => { this.pending--; this.error = 'Item could not be removed.'; });
    }
    checkout() {
        if (this.pending || this.checkingOut || !this.productInOrders.length) return;
        if (!this.currentUser) { this.router.navigate(['/login'], {queryParams: {returnUrl: '/cart'}}); return; }
        if (this.currentUser.role !== Role.Customer) { this.error = 'Use a customer account to place an order.'; return; }
        this.checkingOut = true; this.error = '';
        this.cartService.checkout().subscribe(() => {
            this.checkingOut = false; this.productInOrders = []; this.router.navigate(['/order']);
        }, () => { this.checkingOut = false; this.error = 'Checkout failed. Your cart is retained; check stock and try again.'; });
    }
}
