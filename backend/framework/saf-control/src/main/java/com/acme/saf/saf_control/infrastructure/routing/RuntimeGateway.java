package com.acme.saf.saf_control.infrastructure.routing;

public interface RuntimeGateway {
    void dispatch(RuntimeMessageEnvelope envelope);
}
