package com.example.microservices.core.route;

import com.example.microservices.core.route.persistence.RouteEntity;
import com.example.microservices.core.route.persistence.RouteRepository;
import com.example.microservices.core.route.services.RouteServiceCacheable;
import com.example.microservices.core.route.util.ContainersTestBase;
import com.example.microservices.core.route.util.RouteEntityHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@Slf4j
@SpringBootTest( webEnvironment = RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = NOT_SUPPORTED)
public class RouteServiceCacheableTest extends ContainersTestBase {
    @Autowired
    private CacheManager cacheManager;

    @Value("${spring.cache.cache-names}")
    private String redisName;

    @Autowired
    private RouteServiceCacheable serviceCacheable;

    @SpyBean
    @Autowired
    private RouteRepository routeRepository;

    @BeforeEach
    void setup() {
        routeRepository.deleteAll();
    }

    @Test
    void testFindByRouteId() {
        final int routeId = 1;
        //
        routeRepository.save(RouteEntityHelper.getEntity(routeId));
        cacheManager.getCache(redisName).clear();

        // первое обращение к сервису, получение данных из БД: findByRouteId + 1
        assertTrue(serviceCacheable.findByRouteId(routeId).isPresent());

        // данные извлекаются из БД, так как это первый раз и данные не кешированы.
        verify(routeRepository, times(1)).findByRouteId(routeId);

        // второе обращение к сервису, получение данных из кэша и нет обращения к БД
        assertTrue(serviceCacheable.findByRouteId(routeId).isPresent());

        // данные из БД не извлекаются так как при первом обращении, по этому findByRouteId по прежнему 1
        verify(routeRepository, times(1)).findByRouteId(routeId);
    }

    @Test
    void testFindByFromStoreIdAndToStoreId(){
        //
        final int routeIdToDelete = 2;
        routeRepository.save(RouteEntityHelper.getEntity(routeIdToDelete));
        routeRepository.save(RouteEntityHelper.getEntity(3));
        routeRepository.save(RouteEntityHelper.getEntity(4));
        routeRepository.save(RouteEntityHelper.getEntity(5));
        cacheManager.getCache(redisName).clear();


        // найти все пути от пункта до пункта. Репо + 1, Сервису + 1
        assertEquals(3, serviceCacheable.findByFromStoreIdAndToStoreId(10, 15).size());
        // найти все пути от пункта до пункта. Репо + 0, Сервису + 1
        assertEquals(3, serviceCacheable.findByFromStoreIdAndToStoreId(10, 15).size());

        // взяли из БД только 1 раз, второй раз из кэш
        verify(routeRepository, times(1)).findByFromStoreIdAndToStoreId(10, 15);

        // удалим одну запись из БД, это очистит кэш. Обращаемся к сервису но не к ф-ии удаления, а это не считаем
        RouteEntity entity = serviceCacheable.findByRouteId(routeIdToDelete).get();
        serviceCacheable.delete(entity);

        // снова вызовем список путей и т.к. кэш пуст, снова обратимся к Репо +1 и путей на один меньше
        assertEquals(2, serviceCacheable.findByFromStoreIdAndToStoreId(10, 15).size());

        // кэш очищался по этому обращались к БД 2 раза
        verify(routeRepository, times(2)).findByFromStoreIdAndToStoreId(10, 15);
    }

    @Test()
    void testDelete(){
        final int routeId = 6;
        //
        routeRepository.save(RouteEntityHelper.getEntity(routeId));
        cacheManager.getCache(redisName).clear();

        // получаем сущность и это первое обращение к БД - оно кэшируется, сервис +1, репо +1
        Optional<RouteEntity> entity = serviceCacheable.findByRouteId(routeId);
        assertTrue(entity.isPresent());

        // удаляем сущность из БД и очищаем кэш
        serviceCacheable.delete(entity.get());

        // пытаемся получить сущность но при удалении кэш очистился и идем в БД
        assertTrue(serviceCacheable.findByRouteId(routeId).isEmpty());

        // должно быть 2 обращение к БД до удаления и после
        verify(routeRepository, times(2)).findByRouteId(routeId);
    }

    @Test
    void testSave(){
        final int routeId = 7;
        // первая запись инициализация
        routeRepository.save(RouteEntityHelper.getEntity(routeId));
        cacheManager.getCache(redisName).clear();

        // получаем сущность и это первое обращение к БД - оно кэшируется
        Optional<RouteEntity> entity = serviceCacheable.findByRouteId(routeId);
        assertTrue(entity.isPresent());

        // изменяем сущность и записываем в БД
        RouteEntity route = entity.get();
        route.setMinutesFromTo(22);
        serviceCacheable.save(route);

        // 2 раз получаем сущность не из БД, а из кэша так как он обновился при записи
        Optional<RouteEntity> entityNew = serviceCacheable.findByRouteId(routeId);
        assertTrue(entityNew.isPresent());
        RouteEntity routeNew = entityNew.get();
        assertEquals(22, routeNew.getMinutesFromTo());

        // должно быть 1 обращение к БД тк при обновлении кэш обновился
        verify(routeRepository, times(1)).findByRouteId(routeId);
    }
}
