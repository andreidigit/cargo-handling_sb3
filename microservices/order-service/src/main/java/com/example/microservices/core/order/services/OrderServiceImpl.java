package com.example.microservices.core.order.services;

import com.example.microservices.core.order.broker.ProducerRevise;
import com.example.microservices.core.order.invariant.RuleOrderDelete;
import com.example.microservices.core.order.invariant.RuleOrderUpdate;
import com.example.microservices.core.order.persistence.OrderEntity;
import com.example.microservices.core.order.persistence.OrderRepository;
import com.example.mutual.api.core.order.Order;
import com.example.mutual.api.core.order.OrderService;
import com.example.mutual.api.exceptions.InvalidInputException;
import com.example.mutual.api.exceptions.NotFoundException;
import com.example.mutual.util.http.ServiceUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.Optional;

import static java.util.logging.Level.FINE;

@Slf4j
@RestController
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final ServiceUtil serviceUtil;
    private final Scheduler jdbcScheduler;
    private final ProducerRevise producer;
    private final List<RuleOrderUpdate> rulesUpdate;
    private final List<RuleOrderDelete> rulesDelete;

    @Autowired
    public OrderServiceImpl(
            OrderRepository repository,
            OrderMapper mapper,
            ServiceUtil serviceUtil,
            @Qualifier("jdbcScheduler") Scheduler jdbcScheduler,
            ProducerRevise producer,
            List<RuleOrderUpdate> rulesUpdate,
            List<RuleOrderDelete> rulesDelete
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
        this.jdbcScheduler = jdbcScheduler;
        this.producer = producer;
        this.rulesUpdate = rulesUpdate;
        this.rulesDelete = rulesDelete;
    }

    @Override
    public Mono<Order> getOrder(int orderId) {
        if (orderId < 1) {
            throw new InvalidInputException("Invalid orderId: " + orderId);
        }
        log.info("Will get order with id={}", orderId);
        return Mono.fromCallable(() -> internalGetStore(orderId))
                .switchIfEmpty(Mono.error(new NotFoundException("No order found for orderId: " + orderId)))
                .log(log.getName(), FINE)
                .subscribeOn(jdbcScheduler);
    }

    private Order internalGetStore(int orderId) {
        return repository.findByOrderId(orderId)
                .map(orderEntity -> {
                    Order order = mapper.entityToApi(orderEntity);
                    order.setServiceAddress(serviceUtil.getServiceAddress());
                    return order;
                })
                .orElse(null);
    }

    @Transactional
    @Override
    public Mono<Order> createOrder(Order body) {
        if (body.getOrderId() < 1) {
            throw new InvalidInputException("Invalid orderId: " + body.getOrderId());
        }
        return Mono.fromCallable(()-> internalCreateOrder(body))
                .subscribeOn(jdbcScheduler);
    }

    private Order internalCreateOrder(Order body) {
        try {
            OrderEntity entity = mapper.apiToEntity(body);
            OrderEntity newEntity = repository.save(entity);
            producer.orderCreated(newEntity);
            log.debug("createOrder: created a order entity: {}", body.getOrderId());
            return mapper.entityToApi(newEntity);
        } catch (DataIntegrityViolationException dive) {
            throw new InvalidInputException("Duplicate key, Order Id: " + body.getOrderId());
        }
    }

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 100)
    )
    @Override
    public Mono<Order> updateOrder(Order body) {
        if (body.getOrderId() < 1) {
            throw new InvalidInputException("Invalid orderId: " + body.getOrderId());
        }
        return Mono.fromCallable(()-> internalUpdateOrder(body))
                .subscribeOn(jdbcScheduler);
    }
    private Order internalUpdateOrder(Order body) {
        Optional<OrderEntity> entityOpt = repository.findByOrderId(body.getOrderId());
        if (entityOpt.isPresent()) {
            OrderEntity entity = entityOpt.get();
            OrderEntity oldOrderEntity = mapper.cloneOrderEntity(entity);
            boolean isBrokenRule = rulesUpdate.stream()
                    .map(rule -> rule.apply(entity, mapper.apiToEntity(body)))
                    .anyMatch(bool -> !bool);
            if (isBrokenRule) {
                log.warn("updateOrder: there is broken Rule of entity orderId: {}", body.getOrderId());
                throw new InvalidInputException("There is a broken Update Rule, Order Id: " + body.getOrderId());
            }
            repository.save(entity);
            producer.orderUpdated(oldOrderEntity);
            log.debug("updateOrder: updated a order entity: {}", body.getOrderId());
            return mapper.entityToApi(entity);
        } else {
            throw new NotFoundException("There is no Order with orderId: " + body.getOrderId());
        }
    }

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 100)
    )
    @Override
    public Mono<Void> deleteOrder(int orderId) {
        return Mono.fromRunnable(()->internalDeleteOrder(orderId)).subscribeOn(jdbcScheduler).then();
    }

    private void internalDeleteOrder(int orderId) {
        log.debug("deleteOrder: tries to delete with orderId: {}", orderId);
        repository.findByOrderId(orderId).ifPresent(
            orderEntity->{
                boolean isBrokenRule = rulesDelete.stream()
                    .map(rule -> rule.check(orderEntity))
                    .anyMatch(bool -> !bool);
                if (isBrokenRule) {
                    log.warn("deleteOrder: there is broken Rule of entity orderId: {}", orderId);
                    throw new InvalidInputException("There is a broken Delete Rule, order Id: " + orderId);
                }
                repository.delete(orderEntity);
                producer.orderDeleted(orderEntity);
            }
        );
    }
}
