package com.eshop.ordering.api.application.queries;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderQueriesImpl implements OrderQueries {
  private final EntityManager entityManager;

  @Override
  public OrderViewModel.Order getOrder(Long id) {
    var query = entityManager.createNativeQuery("""
          SELECT o.id as orderNumber, o.order_date as date, o.description, o.city, o.country, o.state, o.street,
            o.zip_code as zipCode, o.order_status_id as status,
            oi.product_name, oi.units, oi.unit_price as unitPrice, oi.picture_url as pictureUrl
          FROM orders o
          LEFT JOIN order_item oi on o.id = oi.order_id
          WHERE o.id = ?1
        """).setParameter(1, id);

    return toOrder(query.getResultList());
  }

  @Override
  public List<OrderViewModel.OrderSummary> getOrdersFromUser(String userId) {
    var query = entityManager.createNativeQuery("""
        SELECT o.id as orderNumber, o.order_date as date, o.order_status_id as status, sum(oi.units * oi.unit_price) as total
        FROM orders o
        LEFT JOIN order_item oi ON o.id = oi.order_id
        LEFT JOIN buyer ob on o.buyer_id = ob.id
        WHERE ob.identity_guid = ?1
        GROUP BY o.id, o.order_date, o.order_status_id
        ORDER BY o.id
        """).setParameter(1, userId);

    return toOrderSummaries(query.getResultList());
  }

  @Override
  public List<OrderViewModel.CardType> getCardTypes() {
    // TODO HD create db tables
//    var query = entityManager.createNativeQuery(
//        "SELECT * FROM cardtypes",
//        OrderViewModel.CardType.class
//    );
//    return (List<OrderViewModel.CardType>) query.getResultList();
    return Collections.emptyList();
  }

  private List<OrderViewModel.OrderSummary> toOrderSummaries(List<Object[]> result) {
    return result.stream()
        .map(r -> {
          var id = (BigInteger) r[0];
          var date = (Timestamp) r[1];
          var status = (Integer) r[2];
          var total = (Double) r[3];

          return new OrderViewModel.OrderSummary(
              id.longValue(),
              date.toLocalDateTime(),
              status,
              total
          );
        }).collect(Collectors.toList());
  }

  private OrderViewModel.Order toOrder(List<Object[]> result) {
    var orderItems = result.stream().map(r -> {
      var productName = (String) r[9];
      var units = (Integer) r[10];
      var unitPrice = (Double) r[11];
      var pictureUrl = (String) r[12];

      return new OrderViewModel.OrderItem(
          productName,
          units,
          unitPrice,
          pictureUrl
      );
    }).collect(Collectors.toList());

    var r = result.get(0);

    var orderId = (BigInteger) r[0];
    var date = (Timestamp) r[1];
    var description = (String) r[2];
    var city = (String) r[3];
    var country = (String) r[4];
    var state = (String) r[5];
    var street = (String) r[6];
    var zipCode = (String) r[7];
    var status = (Integer) r[8];

    var total = orderItems.stream()
        .map(item -> item.units() * item.unitPrice())
        .reduce(Double::sum)
        .orElse(0D);

    return new OrderViewModel.Order(
        orderId.longValue(),
        date.toLocalDateTime(),
        status,
        description,
        street,
        city,
        state,
        zipCode,
        country,
        orderItems,
        total
    );
  }
}
