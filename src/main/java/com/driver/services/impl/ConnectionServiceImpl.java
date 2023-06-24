package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{
        User user=userRepository2.findById(userId).get();
        if(user.getConnected()==true)
            throw new Exception("Already connected");
        else if(user.getCountry().getCountryName().toString().equalsIgnoreCase(countryName))
            return user;
        List<ServiceProvider> serviceProviders=user.getServiceProviderList();
        ServiceProvider lowestServiceProvider=null;
        int lowestId=Integer.MAX_VALUE;
        for(ServiceProvider serviceProvider:serviceProviders){
            List<Country> countries=serviceProvider.getCountryList();
            for(Country country:countries){
                if(countryName.equalsIgnoreCase(country.getCountryName().toString())){
                    if(lowestServiceProvider==null || lowestId > serviceProvider.getId()){
                        lowestId=serviceProvider.getId();
                        lowestServiceProvider=serviceProvider;
                    }
                }
            }
        }

        if(lowestServiceProvider==null)
            throw new Exception("Unable to connect");
        //make connection
        String maskedIpAd=CountryName.valueOf(countryName.toUpperCase()).toCode()+"."+lowestServiceProvider.getId()+"."+userId;
        user.setMaskedIp(maskedIpAd);
        user.setConnected(true);
        Connection connection=new Connection();
        connection.setServiceProvider(lowestServiceProvider);
        connection.setUser(user);
        user.getConnectionList().add(connection);
        lowestServiceProvider.getConnectionList().add(connection);
        userRepository2.save(user);
        serviceProviderRepository2.save(lowestServiceProvider);
        return user;
    }
    @Override
    public User disconnect(int userId) throws Exception {

        User user=userRepository2.findById(userId).get();
        if(user.getConnected()==false)
            throw new Exception("Already disconnected");
        user.setConnected(false);
        user.setMaskedIp(null);

        return userRepository2.save(user);
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {
        User receiver=userRepository2.findById(receiverId).get();
        CountryName receiverCountryName=null;

        if(receiver.getConnected()){
            String maskedCode= receiver.getMaskedIp().substring(0,3);
            switch (maskedCode) {
                case "001":
                    receiverCountryName = CountryName.IND;
                    break;
                case "002":
                    receiverCountryName = CountryName.USA;
                    break;
                case "003":
                    receiverCountryName = CountryName.AUS;
                    break;
                case "004":
                    receiverCountryName = CountryName.CHI;
                    break;
                case "005":
                    receiverCountryName = CountryName.JPN;
                    break;
            }
        }
        else{
            receiverCountryName=receiver.getCountry().getCountryName();
        }
        User user=null;
        try{
            user=connect(senderId,receiverCountryName.toString());
        }
        catch(Exception e){
            throw new Exception("Cannot establish communication");
        }
        return user;
    }
}
