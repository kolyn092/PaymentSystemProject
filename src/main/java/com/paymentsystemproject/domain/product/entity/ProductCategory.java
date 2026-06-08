package com.paymentsystemproject.domain.product.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;

@Getter
public enum ProductCategory {
	ELECTRONIC("전자기기"),
	CLOTHES("의류"),
	FOOD("식품");

	private final String category;

	ProductCategory(String category) {
		this.category = category;
	}

	@JsonCreator
	public static ProductCategory from(String str) {
		for (ProductCategory category : ProductCategory.values()) {
			if (category.getCategory().equals(str)) {
				return category;
			}
		}
		throw new IllegalStateException("일치하는 카테고리가 없습니다.");
	}
}
