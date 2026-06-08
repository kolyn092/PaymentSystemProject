package com.paymentsystemproject.domain.product.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;

@Getter
public enum ProductStatus {
	ON_SALE("판매중"),
	SOLD_OUT("품절");

	private final String productStatus;

	ProductStatus(String status) {
		this.productStatus = status;
	}

	@JsonCreator
	public static ProductStatus from(String str) {
		for (ProductStatus status : ProductStatus.values()) {
			if (status.getProductStatus().equals(str)) {
				return status;
			}
		}
		throw new IllegalStateException("일치하는 상품 상태가 없습니다.");
	}
}
