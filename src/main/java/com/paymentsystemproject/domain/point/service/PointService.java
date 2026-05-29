package com.paymentsystemproject.domain.point.service;

import org.springframework.stereotype.Service;

import com.paymentsystemproject.domain.point.repository.PointRepository;

@Service
public class PointService {

	private final PointRepository pointRepository;

	public PointService(PointRepository pointRepository) {
		this.pointRepository = pointRepository;
	}
}
