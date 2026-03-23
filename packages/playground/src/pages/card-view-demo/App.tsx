import { useCallback, useEffect, useState } from '@lynx-js/react'
import './App.css'

import { close } from 'sparkling-navigation';
import * as storage from 'sparkling-storage';

interface ProductCard {
  id: number;
  emoji: string;
  tag: string;
  title: string;
  description: string;
  price: string;
  rating: number;
}

const PRODUCTS: ProductCard[] = [
  {
    id: 1,
    emoji: '☕',
    tag: 'Best Seller',
    title: 'Signature Espresso',
    description: 'Rich, bold espresso crafted from single-origin beans roasted in small batches.',
    price: '$4.99',
    rating: 4.9,
  },
  {
    id: 2,
    emoji: '🍵',
    tag: 'New Arrival',
    title: 'Matcha Latte',
    description: 'Ceremonial-grade matcha whisked to perfection with creamy oat milk.',
    price: '$5.49',
    rating: 4.7,
  },
  {
    id: 3,
    emoji: '🧁',
    tag: 'Limited Edition',
    title: 'Velvet Cupcake',
    description: 'Red velvet cupcake topped with cream cheese frosting and edible gold.',
    price: '$6.25',
    rating: 5.0,
  },
];

export function App(props: { onMounted?: () => void }) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [showToast, setShowToast] = useState(false);

  const product = PRODUCTS[currentIndex];

  useEffect(() => {
    props.onMounted?.();
  }, []);

  const handleAddToCart = useCallback(() => {
    storage.setItem(
      {
        key: 'cart_item',
        data: product,
      },
      () => {
        setShowToast(true);
        setTimeout(() => setShowToast(false), 1500);
      }
    );
  }, [product]);

  const handleNext = useCallback(() => {
    setCurrentIndex((prev) => (prev + 1) % PRODUCTS.length);
  }, []);

  const onClose = useCallback(() => {
    close();
  }, []);

  const renderStars = (rating: number) => {
    const stars = [];
    for (let i = 0; i < Math.floor(rating); i++) {
      stars.push(
        <text key={i} className="star">
          ★
        </text>
      );
    }
    return stars;
  };

  return (
    <view>
      <view className="App">
        <view className="Header">
          <text className="Title">Card View Demo</text>
          <text className="Subtitle">Swipe through featured products</text>
        </view>

        <view className="Content">
          <view className="card-wrapper">
            <view className="product-card">
              <view className="card-image-area">
                <text className="card-image-placeholder">{product.emoji}</text>
              </view>

              <view className="card-content">
                <view className="card-tag">
                  <text className="tag-text">{product.tag}</text>
                </view>
                <text className="card-title">{product.title}</text>
                <text className="card-desc">{product.description}</text>

                <view className="card-meta">
                  <text className="card-price">{product.price}</text>
                  <view className="card-rating-row">
                    {renderStars(product.rating)}
                    <text className="rating-num">{product.rating}</text>
                  </view>
                </view>
              </view>
            </view>

            <view className="action-btn" bindtap={handleNext}>
              <text className="action-btn-text">
                Next Product ({currentIndex + 1}/{PRODUCTS.length})
              </text>
            </view>

            <view className="action-btn" bindtap={handleAddToCart}>
              <text className="action-btn-text">Add to Cart</text>
            </view>

            <view className="close-btn" bindtap={onClose}>
              <text className="close-btn-text">Close</text>
            </view>
          </view>
        </view>

        {showToast && (
          <view className="toast">
            <text className="toast-text">Added {product.title} to cart!</text>
          </view>
        )}
      </view>
    </view>
  );
}
