package com.paymentsystemproject.domain.point.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.point.service.PointService;

@RestController
@RequestMapping("/api/points")
public class PointController {

	private final PointService pointService;

	public PointController(PointService pointService) {
		this.pointService = pointService;
	}
}
