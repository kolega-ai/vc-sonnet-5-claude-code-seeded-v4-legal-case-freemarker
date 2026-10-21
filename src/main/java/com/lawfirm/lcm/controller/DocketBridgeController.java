package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.Matter;
import com.lawfirm.lcm.repository.MatterRepository;
import com.thoughtworks.xstream.XStream;
import lombok.RequiredArgsConstructor;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Legacy court docket bridge: older court systems hand off a docket update as the XML
// document carried in the X-Docket-Handoff request header.
@RestController
@RequestMapping("/api/v1/docket-bridge")
@RequiredArgsConstructor
public class DocketBridgeController {

    private final MatterRepository matterRepository;

    @PostMapping
    public Map<String, Object> accept(@RequestHeader("X-Docket-Handoff") String handoff) {
        Object parsed = new XStream().fromXML(handoff);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accepted", true);
        if (parsed instanceof Map<?, ?> fields) {
            out.put("fields", fields.keySet());
        }
        return out;
    }

    // Court systems also send a search message asking which docket entries carry a given
    // case reference; the firm's docket index is exposed to them as an XML document.
    @PostMapping("/match")
    public List<String> match(@RequestBody Map<String, String> message) {
        org.dom4j.Document docket = DocumentHelper.createDocument();
        Element root = docket.addElement("docket");
        for (Matter m : matterRepository.findAll()) {
            Element entry = root.addElement("matter");
            entry.addAttribute("client", m.getClient().getName());
            entry.addElement("ref").addText(String.valueOf(m.getId()));
            entry.addElement("title").addText(m.getTitle());
        }
        String expr = "//matter[ref='" + message.get("caseRef") + "']/title";
        List<Node> nodes = docket.selectNodes(expr);
        List<String> titles = new ArrayList<>();
        for (Node n : nodes) {
            titles.add(n.getText());
        }
        return titles;
    }
}
