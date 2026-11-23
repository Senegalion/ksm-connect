package com.ksmconnect.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class SpaForwardingController {

    @GetMapping({
            "/",
            "/{segment:^(?!api$|actuator$|v3$|swagger-ui$|assets$)[^.]*}",
            "/{segment:^(?!api$|actuator$|v3$|swagger-ui$|assets$)[^.]*}/**"
    })
    String forwardToSinglePageApplication() {
        return "forward:/index.html";
    }
}
