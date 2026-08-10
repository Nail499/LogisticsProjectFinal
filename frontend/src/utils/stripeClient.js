import { loadStripe } from '@stripe/stripe-js';
import axiosClient from '../api/axiosClient';

// Stripe.js-i bir dəfə yükləyib (publishable key backend-dən alınır,
// frontend-də heç bir açar hardcode olunmur) bütün ödəniş formaları
// arasında paylaşan tək nüsxə (bax PaymentForm.jsx). loadStripe() öz-özünə
// nəticəni keşləyir, ona görə modul səviyyəsində saxlamaq kifayətdir.
let stripePromise = null;

export function getStripePromise() {
  if (!stripePromise) {
    stripePromise = axiosClient.get('/api/customer/payments/config').then((res) =>
      loadStripe(res.data.publishableKey)
    );
  }
  return stripePromise;
}
