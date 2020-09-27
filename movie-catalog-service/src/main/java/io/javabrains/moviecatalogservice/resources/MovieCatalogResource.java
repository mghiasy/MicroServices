package io.javabrains.moviecatalogservice.resources;

import io.javabrains.moviecatalogservice.models.CatalogItem;
import io.javabrains.moviecatalogservice.models.Movie;
import io.javabrains.moviecatalogservice.models.Rating;
import io.javabrains.moviecatalogservice.models.UserRating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/catalog")
public class MovieCatalogResource {
    @Autowired //is by type => somewhere there is a bean of type RestTemplate , inject it here
    //if there is more than 1 bean with the same type, we should tag them with qualifier
    //both bean and Autowired
    private RestTemplate restTemplate; //create a property of class of the same type of Bean

    @Autowired
    private WebClient.Builder webClientBuilder;


    @RequestMapping("/{userId}")
    public List<CatalogItem> getCatalog(@PathVariable("userId") String userId){
        //create an instance of rest template ( a utility object that is supposed to make rest api call)
        //RestTemplate restTemplate = new RestTemplate();

        //1.get all the rated movie id
        UserRating ratings = restTemplate.getForObject("http://localhost:8083/ratingsdata/users/" +userId,UserRating.class);

        //2."for each movie" id "call info service" and get detail
        return ratings
                .getUserRatings()//get the list from object
                .stream()
                //for each movie make a separate call and return the "movie"
                //these call are sync => to be async(at the same time) => use webClient
                .map(rating -> {
                    //"option"+"cmd"+"v" => to create a variable name for it
                    Movie movie = restTemplate.getForObject("http://localhost:8082/movies/" + rating.getMovieId(), Movie.class);

                    //TO GET DATA FROM **** WEB CLIENT ****:
                    //Is a REACTIVE STREAM
                    Movie movieFromWebClient = webClientBuilder.build()
                            .get() //type of method
                            .uri("http://localhost:8082/movies/" + rating.getMovieId()) //the path of service
                            .retrieve() //fetch this data
                            .bodyToMono(Movie.class) //convert the body to this instance of this class
                            //mono in async way is same as a proxy
                            .block(); //blocks the execution until the result gets back and mono is fulfilled


                    //3. put them all together
                    //instead of movie can be "movieFromWebClient"
                    return new CatalogItem(movie.getMovieName(),"desc",rating.getRating());

                })
                .collect(Collectors.toList());
    }
}
