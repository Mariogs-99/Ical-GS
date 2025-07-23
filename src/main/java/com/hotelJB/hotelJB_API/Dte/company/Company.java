package com.hotelJB.hotelJB_API.Dte.company;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String nit;
    private String nrc;

    @Column(name = "cod_actividad")
    private String codActividad;

    @Column(name = "desc_actividad")
    private String descActividad;

    @Column(name = "nombre_comercial")
    private String nombreComercial;

    @Column(name = "tipo_establecimiento")
    private String tipoEstablecimiento;

    private String departamento;
    private String municipio;
    private String direccion;
    private String telefono;
    private String correo;

    @Column(name = "cod_estable_mh")
    private String codEstableMh;

    @Column(name = "cod_estable")
    private String codEstable;

    @Column(name = "cod_punto_venta_mh")
    private String codPuntoVentaMh;

    @Column(name = "cod_punto_venta")
    private String codPuntoVenta;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "dte_enabled")
    private boolean dteEnabled = false;

    @Column(name = "mh_password")
    private String mhPassword;

    @Column(name = "cert_password")
    private String certPassword;


}
