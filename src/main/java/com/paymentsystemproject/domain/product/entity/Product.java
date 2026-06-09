package com.paymentsystemproject.domain.product.entity;

import com.paymentsystemproject.global.entity.BaseTimeEntity;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 200, nullable = false)
	private String name;

	@Column(nullable = false, columnDefinition = "int UNSIGNED")
	private int price;

	@Column(nullable = false, columnDefinition = "int UNSIGNED DEFAULT 0")
	private int stock;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	private ProductStatus status;

	@Enumerated(EnumType.STRING)
	private ProductCategory category;

	private Product(String name, int price, int stock, String description, ProductStatus status,
		ProductCategory category) {
		this.name = name;
		this.price = price;
		this.stock = stock;
		this.description = description;
		this.status = status;
		this.category = category;
	}

	public static Product from(String name, int price, int stock, String description, ProductStatus status,
		ProductCategory category) {
		return new Product(name, price, stock, description, status, category);
	}

	public void restoreStock(int quantity) {
		if (quantity <= 0) {
			throw new BusinessException(ErrorCode.INVALID_QUANTITY);
		}
		this.stock += quantity;
	}

	public void decreaseStock(int quantity) {
		if (quantity <= 0) {
			throw new BusinessException(ErrorCode.INVALID_QUANTITY);
		}
		if (quantity > this.stock) {
			throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
		}
		this.stock -= quantity;
	}

	public void increaseStock(int quantity) {
		if (quantity <= 0) {
			throw new BusinessException(ErrorCode.INVALID_QUANTITY);
		}
		this.stock += quantity;
	}
}
