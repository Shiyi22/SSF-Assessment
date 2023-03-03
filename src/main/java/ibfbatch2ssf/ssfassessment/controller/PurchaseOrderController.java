package ibfbatch2ssf.ssfassessment.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import ibfbatch2ssf.ssfassessment.model.Cart;
import ibfbatch2ssf.ssfassessment.model.Delivery;
import ibfbatch2ssf.ssfassessment.model.Item;
import ibfbatch2ssf.ssfassessment.model.Quotation;
import ibfbatch2ssf.ssfassessment.model.Invoice;
import ibfbatch2ssf.ssfassessment.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class PurchaseOrderController {

    // autowire service
    @Autowired
    private PurchaseService purchaseSvc;

    @Autowired
    private QuotationService quotationSvc; 

    // landing page = view1
    @GetMapping(path={"/", "view1.html"})
    public String getLanding(Model model, HttpSession session) {

        // check if cart has item using session
        Cart cart = (Cart) session.getAttribute("cart"); 
        if (null == cart) {
            cart = new Cart(); 
            session.setAttribute("cart", cart);
        }
        model.addAttribute("item", new Item()); 
        model.addAttribute("cart", cart);   

        return "view1"; 
    }

    // post add button 
    @PostMapping("/")
    public String postAdd(@Valid Item item, BindingResult result, Model model, HttpSession session) {

        Cart cart = (Cart) session.getAttribute("cart"); 

        // perform error check
        if (result.hasErrors()) {
            // reset page 
            model.addAttribute("item", item);
            model.addAttribute("cart", cart); 
            return "view1"; 
        }
        // perform aggregation 
        cart = purchaseSvc.aggregate(cart, item); 
        System.out.println(">>> cart contents after aggregation: " + cart); 
        session.setAttribute("cart", cart);
        session.setAttribute("item", item);

        model.addAttribute("item", item); 
        model.addAttribute("cart", cart);        



        return "view1"; 
    }

    // next button pressed
    @GetMapping("/shippingaddress")
    public String getAddress(Model model, HttpSession session) {

        Cart cart = (Cart) session.getAttribute("cart"); 
        System.out.println(cart); 

        // cannot navigate to view 2 without a valid cart
        if (cart.getContents().isEmpty()) {
            model.addAttribute("item", new Item());
            model.addAttribute("cart", cart);            
            return "view1"; 
        }

        // task 2 
        model.addAttribute("delivery", new Delivery());

        Delivery delivery = (Delivery) session.getAttribute("delivery"); 
        if (null == delivery) {
            delivery = new Delivery(); 
            session.setAttribute("delivery", delivery);
        }

        model.addAttribute("delivery", delivery);      

        return "view2"; 
    }

    @PostMapping("/quotation")
    public String postAddress(@Valid Delivery delivery, BindingResult result, HttpSession session, Model model) throws Exception { 

        // check for delivery details errors 
        if (result.hasErrors()) {
            model.addAttribute("delivery", delivery); 
            return "view2"; 
        }

        // if no error, submit to POApp 
        // create List<String> as the list of items from customer cart 
        Cart cart = (Cart) session.getAttribute("cart"); 
        List<String> items = quotationSvc.getList(cart); 
        System.out.println(items);
        
        // make HTTP call to get quotation (svc)
        Quotation quote = quotationSvc.getQuotations(items); 

        // check for error message
        // if (quote.getQuoteId().isEmpty()) {

        //     JsonObject json = Json.createObjectBuilder().add("error", "error message").build(); 
        //     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json.toString()); 
        // }

        // create invoice and bind model data
        Invoice invoice = purchaseSvc.createInvoice(quote, delivery, cart); 
        model.addAttribute("invoice", invoice); 
        
        // clear cart contents 
        session.invalidate();

        return "view3"; 
    }
    
}
